package com.peraglobal.web.controller;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.peraglobal.common.toolkit.Constant;
import com.peraglobal.core.dto.BootTablePage;
import com.peraglobal.core.model.KUser;
import com.peraglobal.web.service.TransMonitorService;
import com.peraglobal.web.utils.JsonUtils;

@RestController
@RequestMapping("/trans/monitor/")
public class TransMonitorController {
	
	@Autowired
	private TransMonitorService transMonitorService;
	
	@RequestMapping("getList.shtml")
	public String getList(Integer offset, Integer limit, HttpServletRequest request){
		KUser kUser = (KUser) request.getSession().getAttribute(Constant.SESSION_ID);
		BootTablePage list = transMonitorService.getList(offset, limit, kUser.getuId());				
		return JsonUtils.objectToJson(list);
	}
	
	@RequestMapping("getAllMonitorTrans.shtml")
	public String getAllMonitorJob(HttpServletRequest request){
		KUser kUser = (KUser) request.getSession().getAttribute(Constant.SESSION_ID);
		return JsonUtils.objectToJson(transMonitorService.getAllMonitorTrans(kUser.getuId()));
	}
	
	@RequestMapping("getAllSuccess.shtml")
	public String getAllSuccess(HttpServletRequest request){
		KUser kUser = (KUser) request.getSession().getAttribute(Constant.SESSION_ID);
		return JsonUtils.objectToJson(transMonitorService.getAllSuccess(kUser.getuId()));
	}
	
	@RequestMapping("getAllFail.shtml")
	public String getAllFail(HttpServletRequest request){
		KUser kUser = (KUser) request.getSession().getAttribute(Constant.SESSION_ID);
		return JsonUtils.objectToJson(transMonitorService.getAllFail(kUser.getuId()));
	}
}
