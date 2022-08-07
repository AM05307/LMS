package com.rad.main;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rad.util.MessageManager;
import com.rationalowl.minerva.appserver.AppServerManager;

import sun.misc.BASE64Encoder;

public class SmsSendProcess extends Thread
{
	static Logger logger = Logger.getLogger(SmsSendProcess.class);

	private IMainService		mainService;

	private List<Map<String, String>> recvList;
	private Map<String, String> params;

	public SmsSendProcess(IMainService mainService, List<Map<String, String>> recvList, Map<String, String> params)
	{
		this.mainService = mainService;
		this.recvList = recvList;
		this.params = params;
	}

	public void run(){
		logger.info("start SmsSendProcess... sender[" + this.params.get("user_id") + "]" );

		try {
			boolean isSuccess = false;
			int i=0;
			System.out.println("###### recvList=" + recvList);
			for(Map recvMap : recvList) {

				String is_emergency = this.params.get("is_emergency");
				String regist_id = (String)recvMap.get("regist_id");
				String forced_sms = this.params.get("forced_sms");
				recvMap.put("user_id", this.params.get("user_id"));
				recvMap.put("user_name", this.params.get("user_name"));
				recvMap.put("user_role", this.params.get("user_role"));
				recvMap.put("title", this.params.get("title"));
				recvMap.put("message", this.params.get("message"));
				recvMap.put("category", this.params.get("category"));
				System.out.println("###### is_emergency=" + is_emergency + ", forced_sms=" + forced_sms + ", regist_id=" + regist_id);
				if( (is_emergency != null && is_emergency.equals("1")) || (regist_id == null || regist_id.length() == 0) || (forced_sms != null && forced_sms.equals("1"))) {
					String mobile = (String)recvMap.get("mobile");
					if( mobile != null && mobile.length() > 0 ) {
						// 문자발송
						isSuccess = sendMessage( mobile );
					}
					recvMap.put("is_complete", (isSuccess) ? "1" : "0");
					recvMap.put("send_type", "SMS");
				}
				else {
					if( MessageManager.getInstance().isEnabledServer() == true ) {
						isSuccess = sendMulticastMsg( recvMap );
						recvMap.put("is_complete", (isSuccess) ? "1" : "0");
						recvMap.put("send_type", "PUSH");
					}
					else {
						logger.error("MessageServer is not enabled. Please repeat again for a while");
						Thread.sleep(5000);
					}
				}

				Thread.sleep(1000);
			}

			mainService.updateSmsReceiverComplete(recvList);
		}
		catch(Exception e) {
			e.printStackTrace();
		}

		logger.info("finished SmsSendProcess... sender[" + this.params.get("user_id") + "] result=" + this.recvList );
	}

	public void run_KakaoTalk(){
		logger.info("start KakaoTalkSendProcess..." );

		try {
			boolean isSuccess = false;
			int i=0;
			System.out.println("###### recvList=" + recvList);
			for(Map recvMap : recvList) {
				
				if(!recvMap.isEmpty() ) {
					String template_code = (String)recvMap.get("template_code");
					String send_id = (String)recvMap.get("send_id");
					String stdt_name = (String)recvMap.get("stdt_name");
					String stdt_no = String.valueOf(recvMap.get("stdt_no"));
					String curr_name = (String)recvMap.get("curr_name");
					String mobile = (String)recvMap.get("mobile");
					String content = "";
					if("start_notice_2".equals(template_code)) {
						content = stdt_name
								+ "님께서 수강신청하신 "
								+ curr_name
								+ "에 대한 알림입니다.\n\n"
								+ "\n만약 수신을 원치 않는 경우에는 상단의 알림톡 차단을 눌러주세요.";
					} else if("end_notice_3".equals(template_code)) {
						content = stdt_name
								+ "님께서 금일 출석하신 "
								+ curr_name
								+ "에 대한 알림입니다.\n\n"
								+ "\n만약 수신을 원치 않는 경우에는 상단의 알림톡 차단을 눌러주세요.\n";
					} else if("middle_notice".equals(template_code)) {
						content = stdt_name
								+ "님께서 금일 출석하신 "
								+ curr_name
								+ "의 한 알림입니다.\n\n"
								+ "\n만약 수신을 원치 않는 경우에는 상단의 알림톡 차단을 눌러주세요.";
					} else {
						content = "테스트입니다.";
					}
					
					recvMap.put("content", content);
					//카카오 알림톡 발송
					isSuccess = sendKakaoTalk(template_code, content, send_id, stdt_name, stdt_no, curr_name,  mobile );
					recvMap.put("is_complete", (isSuccess) ? "1" : "0");
					System.out.println("카카오톡 발송 후의 recvMap : \n" + recvMap);
				}
				else {
					logger.error("[카카오 알림톡 발송 오류]MessageServer is not enabled. Please repeat again for a while");
					Thread.sleep(5000);
				}

				Thread.sleep(1000);
			}
			// 카카오톡 전송결과 업데이트 
			mainService.updateKakaoTalkReceiverComplete(recvList);
		}
		catch(Exception e) {
			e.printStackTrace();
		}

		logger.info("finished kakaoTalkProcess... sender result=" + this.recvList );
	}
	
	private boolean sendMessage(String receiverNum)
	{
		boolean isSuccess = false;
		OutputStream opstrm = null;
		BufferedReader in = null;
		String site_id = Property.getProperty("default.site_id");
		String U_CODE = "1234";

		try {
			System.out.println("###### U_CODE = " + U_CODE);
			System.out.println("###### start send toNum=" + receiverNum + ", fromNum=" + this.params.get("mobile"));
			BASE64Encoder encoder = new BASE64Encoder();
			String param = "U_TYPE=" + encoder.encode("1".getBytes()) +
						   "&U_CODE=" + encoder.encode(U_CODE.getBytes()) +
						   "&U_SUBJECT=" + encoder.encode(this.params.get("title").getBytes("utf-8")) +
						   "&U_MSG=" + encoder.encode(this.params.get("message").getBytes("utf-8")) +
						   "&U_TO_NUM=" + encoder.encode(receiverNum.getBytes()) +
						   "&U_FROM_NUM=" + encoder.encode(this.params.get("mobile").getBytes());

			System.out.println("###### start send param=" + param);
			URL targetURL = new URL("http://sock.whitesms.co.kr/ext/socket.php");
			URLConnection urlConn = targetURL.openConnection();
			HttpURLConnection hurlc = (HttpURLConnection) urlConn;

			// 헤더값을 설정한다.
			hurlc.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
			// 전달 방식을 설정한다. POST or GET, 기본값은 GET 이다.
			hurlc.setRequestMethod("POST");
			// 서버로 데이터를 전송할 수 있도록 한다. GET방식이면 사용될 일이 없으나, true로
			// 설정하면 자동으로 POST로 설정된다. 기본값은 false이다.
			hurlc.setDoOutput(true);
			// 서버로부터 메세지를 받을 수 있도록 한다. 기본값은 true이다.
			hurlc.setDoInput(true);
			hurlc.setUseCaches(false);
			hurlc.setDefaultUseCaches(false);

			//안됨...
			//PrintWriter out = new PrintWriter(hurlc.getOutputStream());
			//out.println(param);
			//out.flush();
			//out.close();

			System.out.println("###### before send sms");
			opstrm = hurlc.getOutputStream();
			opstrm.write( param.getBytes() );
			opstrm.flush();
			opstrm.close();

			String result = "";
			String buffer = null;
			in = new BufferedReader(new InputStreamReader(hurlc.getInputStream()));
			while ((buffer = in.readLine()) != null) {
				result += buffer;
			}
			in.close();

			if( result != null && result.indexOf("ok:") > -1 ) {
				isSuccess = true;
			}

			System.out.println("###### finished send sms result=" + result);
		}
		catch(Exception e) {
			e.printStackTrace();
		}

		return isSuccess;
	}

	private boolean sendMulticastMsg(Map recvMap) {

		boolean isSuccess = false;

        // data format is json string
        // and jackson json library has used.
        ObjectMapper mapper = new ObjectMapper();
        Map<String, Object> jsonData = new LinkedHashMap<String, Object>();
        jsonData.put("messageId", this.params.get("no"));
        jsonData.put("category", this.params.get("category"));
        jsonData.put("groupId", "1001");
        jsonData.put("messageType", "2");
        jsonData.put("url", "");
        jsonData.put("target_activity", "zzzzzzzzzzz");
        jsonData.put("title", this.params.get("title"));
        jsonData.put("message", this.params.get("message"));
        jsonData.put("senderId", this.params.get("user_id"));
        jsonData.put("senderName", this.params.get("user_name"));

        String jsonStr = null;
        try {
            jsonStr = mapper.writeValueAsString(jsonData);
            // target device registration id
            ArrayList<String> targetDevices = new ArrayList<String>();
            targetDevices.add( (String)recvMap.get("regist_id") );

            AppServerManager serverMgr = AppServerManager.getInstance();
            String requestId = serverMgr.sendMulticastMsg(jsonStr, targetDevices, true, this.params.get("title"), this.params.get("message"));
            if( requestId != null ) {
            	isSuccess = true;
            }
            System.out.println("###### sendMulticastMsg Msg :" + jsonStr + "requestId = " + requestId);
        }
        catch (JsonProcessingException e) {
            e.printStackTrace();
        }

        return isSuccess;
    }
	
	
	
	//드림라인 카카오톡 알림톡 발송 
	private boolean sendKakaoTalk(String template_code, String content, String send_id, String stdt_name, String stdt_no, String curr_name, String receiverNum)
	{
		boolean isSuccess = false;
		OutputStream opstrm = null;
		BufferedReader in = null;
		//회원종류
		String id_type = "MID";
		//회원 ID
		String id = "sms_0";
		// API인증키
		String auth_key = "authhashkeyvalue";
		// 문자종류
		String msg_type = "KAT";
		// 발신프로필키
		String callback_key = "callbackkeyvalue";
		/*발송ID 수신번호
		입력형식: 발송ID|수신번호
		- 발송ID: 카카오톡 메시지에 대한 고유값
		- 수신번호: 숫자만 입력
		Ex) 11111|01012356789*/
		
		
		String send_id_receive_number = send_id + "|" + receiverNum;
		

		// 실패건 재전송 여부
		String resend = "NONE";
		try {
			System.out.println("###### start send KakaoTalk toNum=" + send_id_receive_number );
			BASE64Encoder encoder = new BASE64Encoder();
			String param = "id_type=" + id_type 
							+ "&id=" + id
							+ "&auth_key=" + auth_key
							+ "&msg_type=" + msg_type
							+ "&callback_key=" + callback_key
							+ "&send_id_receive_number=" + send_id_receive_number
							+ "&content=" + content
							+ "&resend=" + resend
							+ "&template_code=" + template_code
							+ "&submit=OK" ;

			System.out.println("###### start send param=" + param);
			URL targetURL = new URL("https://sms.co.kr/API/send_kkt.php");
			URLConnection urlConn = targetURL.openConnection();
			HttpURLConnection hurlc = (HttpURLConnection) urlConn;

			// 헤더값을 설정한다.
			hurlc.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
			// 전달 방식을 설정한다. POST or GET, 기본값은 GET 이다.
			hurlc.setRequestMethod("POST");
			// 서버로 데이터를 전송할 수 있도록 한다. GET방식이면 사용될 일이 없으나, true로
			// 설정하면 자동으로 POST로 설정된다. 기본값은 false이다.
			hurlc.setDoOutput(true);
			// 서버로부터 메세지를 받을 수 있도록 한다. 기본값은 true이다.
			hurlc.setDoInput(true);
			hurlc.setUseCaches(false);
			hurlc.setDefaultUseCaches(false);

			//안됨...
			//PrintWriter out = new PrintWriter(hurlc.getOutputStream());
			//out.println(param);
			//out.flush();
			//out.close();

			System.out.println("###### before send KakaoTalk");
			opstrm = hurlc.getOutputStream();
			opstrm.write( param.getBytes() );
			opstrm.flush();
			opstrm.close();

			String result = "";
			String buffer = null;
			in = new BufferedReader(new InputStreamReader(hurlc.getInputStream()));
			while ((buffer = in.readLine()) != null) {
				result += buffer;
			}
			in.close();

			if( result != null && "0".equals(result) ) {
				isSuccess = true;
			}

			System.out.println("###### finished send kakaoTalk result=" + result);
		}
		catch(Exception e) {
			e.printStackTrace();
		}

		return isSuccess;
	}
	//카카오톡 발송 끝 

}// public class SmsSendProcess 
