package com.peraglobal.web.controller;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.peraglobal.common.toolkit.Constant;
import com.peraglobal.core.dto.BootTablePage;
import com.peraglobal.core.dto.ResultDto;
import com.peraglobal.core.model.KUser;
import com.peraglobal.web.service.JobRecordService;
import com.peraglobal.web.utils.JsonUtils;

@RestController
@RequestMapping("/job/record/")
public class JobRecordController {

	@Autowired
	private JobRecordService jobRecordService;
	
	@RequestMapping("getList.shtml")
	public String getList(Integer offset, Integer limit, Integer JobId, HttpServletRequest request){
		KUser kUser = (KUser) request.getSession().getAttribute(Constant.SESSION_ID);
		BootTablePage list = jobRecordService.getList(offset, limit, kUser.getuId(), JobId);				
		return JsonUtils.objectToJson(list);
	}
	
	@RequestMapping("getLogContent.shtml")
	public String getLogContent(Integer recordId){
		try {
			String logContent = jobRecordService.getLogContent(recordId);
			return ResultDto.success(logContent.replace("\r\n", "<br/>"));
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		} 
	}
}