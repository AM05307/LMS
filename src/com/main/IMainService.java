 package com.rad.main;

import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpSession;

public interface IMainService {
  /** 출결알림 카카오톡 발송 */
	public List selectStdtListKT_start(Map<String, String> params) throws Exception;
}
