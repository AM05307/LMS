package com.main;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Resource;

import org.apache.log4j.Logger;
import org.json.simple.JSONObject;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.context.ApplicationContext;
import org.springframework.scheduling.quartz.QuartzJobBean;

import com.ibatis.sqlmap.client.SqlMapClient;
import com.main.IMainService;

// 카카오톡 알림  
public class AttNoticeKT_9_start_Job extends QuartzJobBean {
	@Resource(name = "connection")
	private	SqlMapClient connection;
	
	static Logger logger = Logger.getLogger(AttNoticeKT_9_start_Job.class);
	
	private ApplicationContext ctx;
	private IMainService mainService;
	
	@Override
	protected void executeInternal(JobExecutionContext jobExecutionContext) throws JobExecutionException {
		// TODO Auto-generated method stub
		List<Map<String, String>> stdt_list = null;
		Map<String, String> params			= null;
		
		try {
			// 이 메소드가 있는 부분이 실행될 부분이다.
			// 반복할 작업 구현
			logger.info("Execute attNoticeKT_9_start_Job");
			System.out.println("#### Execute AttNoticeKT_9_start_Job");
			if( mainService == null ) {
				ctx = (ApplicationContext)jobExecutionContext.getJobDetail().getJobDataMap().get("applicationContext");
				mainService = (IMainService)ctx.getBean("com.main.MainService");
			}
	
			Calendar today = Calendar.getInstance();
			int year = today.get(Calendar.YEAR);
			int mon = today.get(Calendar.MONTH) + 1;
			int day = today.get(Calendar.DATE);

			Map paramMap = new HashMap();
			// String now = year + "" + ((mon<10) ? "0" + mon : mon) + "" + ((day<10) ? "0" + day : day) + "000000";
			// paramMap.put("now", now);

			paramMap.put("classtime_class_time", "09:00");
			// 알림대상목록  
			stdt_list = mainService.selectStdtListKT_start(paramMap);

			SimpleDateFormat sdf1 = new SimpleDateFormat("yyyyMMdd_HHmmss");
			Date now = new Date();
			String nowTime1 = sdf1.format(now);
			String send_id = "";
			String stdt_no = "";
			// 대상 목록을 카카오톡 발송 DB에 추가 
			Map<String,String> recvMap = null;
			for (int k = 0; k < stdt_list.size(); k++) {
				recvMap = stdt_list.get(k);
				stdt_no = String.valueOf(recvMap.get("stdt_no"));
				send_id = stdt_no + "_" + nowTime1;
				// 발송 ID
				recvMap.put("send_id", send_id);
				// 템플릿 코드
				recvMap.put("template_code", "start_notice_2");
				System.out.println("[AttNoticeKT_9_start_Job] 카카오톡 발송 DB에 추가\n("+k +") " + recvMap);

				mainService.addUserToKakaoTalkReceiver(recvMap);
			}
			
			// 카카오 알림톡 발송
			SmsSendProcess processor = new SmsSendProcess(mainService, stdt_list, params);
			processor.run_KakaoTalk();
			
			logger.info("Successfully AttNoticeKT_9_start_Job");
			System.out.println("#### Successfully attNoticeKT_9_start_Job");			
			
		} catch(Exception e) {
			e.printStackTrace();
		}
		
	}

}
