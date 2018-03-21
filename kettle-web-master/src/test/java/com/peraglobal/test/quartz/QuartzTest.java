package com.peraglobal.test.quartz;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

import com.peraglobal.web.quartz.QuartzManager;
import com.peraglobal.web.quartz.TransQuartz;

public class QuartzTest {

	@Test
	public void AddJobTest(){
		
		
		
		
		String cron = "*/5 * * * * ?";
		Map<String, Object> parameter = new HashMap<String, Object>();
		QuartzManager.addJob("aaa", "aaaaaaaaaa", "qqq1111", "aaa", TransQuartz.class, cron, parameter);
	}
	
	
	
	public static void main(String[] args){
		Map<String, Object> parameter = new HashMap<String, Object>();
		QuartzManager.addOnceJob("aaa", "aaaaaaaaaa", "qqq1111", "aaa", TransQuartz.class, parameter);
	}
}
