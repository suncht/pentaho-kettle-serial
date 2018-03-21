package com.peraglobal.web.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.peraglobal.core.dto.ResultDto;
import com.peraglobal.web.service.UserService;
import com.peraglobal.web.utils.JsonUtils;

@RestController
@RequestMapping("/user/")
public class UserController {

	@Autowired
	private UserService userService;
	
	@RequestMapping("getList.shtml")
	public String getList(Integer offset, Integer limit){
		return JsonUtils.objectToJson(userService.getList(offset, limit));
	}
	
	@RequestMapping("delete.shtml")
	public String delete(Integer uId){
		userService.delete(uId);
		return ResultDto.success();
	}
	
	@RequestMapping("resetPassword.shtml")
	public String resetPassword(){
		
		return ResultDto.success();
	}
}
