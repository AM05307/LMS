package com.main;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import javax.annotation.Resource;
import javax.servlet.http.HttpSession;

import org.springframework.stereotype.Service;
import com.ibatis.sqlmap.client.SqlMapClient;

@Service("com.rad.main.MainService")
public class MainService implements IMainService
{
	@Resource(name = "connection")
	private	SqlMapClient connection;

	/** 카카오 알림톡 */
	@Override
	public List selectStdtListKT_start(Map<String, String> params) throws Exception {
		List result = null;
		try {
			result = connection.queryForList("rad.main.selectStdtListKT_start", params);
		} catch (Exception e) {
			e.printStackTrace();
			throw e;
		}
		return result;
	}
  
 }
