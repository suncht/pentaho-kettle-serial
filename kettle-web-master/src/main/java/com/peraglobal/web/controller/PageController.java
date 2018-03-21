package com.peraglobal.web.controller;

import javax.servlet.http.HttpSession;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/view/")
public class PageController {

	
	@RequestMapping("indexUI.shtml")
	public String IndexUI(){
		return "index";
	}
	
	@RequestMapping("mainUI.shtml")
	public String mainUI(){
		return "main";
	}
	
	@RequestMapping("loginUI.shtml")
	public String loginUI(@ModelAttribute("errorMsg") String errorMsg, HttpSession session){
		session.setAttribute("errorMsg", errorMsg);
		return "login";
	}
	
	/**==========repository start==========**/
	
	@RequestMapping("repostory/listUI.shtml")
	public String repostoryListUI(){
		return "repository/list";
	}
	
	@RequestMapping("repository/addUI.shtml")
	public String repositoryAddUI(){
		return "repository/add";
	}
	
	@RequestMapping("repository/editUI.shtml")
	public String repositoryEditUI(Integer repositoryId, Model model){
		model.addAttribute("repositoryId", repositoryId);
		return "repository/edit";
	}
	
	/**==========repository end==========**/
	
	/**==========job start==========**/
	
	@RequestMapping("job/listUI.shtml")
	public String jobListUI(){
		return "job/list";
	}	
	
	@RequestMapping("job/rAddUI.shtml")
	public String jobRAddUI(){
		return "job/r-add";
	}	
	
	@RequestMapping("job/fAddUI.shtml")
	public String jobFAddUI(){
		return "job/f-add";
	}
	
	@RequestMapping("job/editUI.shtml")
	public String jobEditUI(Integer jobId, Model model){
		model.addAttribute("jobId", jobId);
		return "job/edit";
	}
	
	/**==========job end==========**/	
	
	/**==========trans start ==========**/
	
	@RequestMapping("trans/listUI.shtml")
	public String transListUI(){
		return "trans/list";
	}
	
	@RequestMapping("trans/rAddUI.shtml")
	public String transRAddUI(){
		return "trans/r-add";
	}
	
	@RequestMapping("trans/fAddUI.shtml")
	public String transFAddUI(){
		return "trans/f-add";
	}
	
	@RequestMapping("trans/editUI.shtml")
	public String transEditUI(Integer transId, Model model){
		model.addAttribute("transId", transId);
		return "trans/edit";
	}
	
	/**==========trans end ==========**/
	
	/**==========user start==========**/
	
	@RequestMapping("user/listUI.shtml")
	public String userListUI(){
		return "user/list";
	}
	
	@RequestMapping("user/addUI.shtml")
	public String userAddUI(){
		return "user/add";
	}
	
	@RequestMapping("user/editUI.shtml")
	public String userEditUI(Integer userId, Model model){
		model.addAttribute("uId", userId);
		return "user/edit";
	}
	
	/**==========user end==========**/
	
	/**==========record start==========**/
	
	@RequestMapping("trans/record/listUI.shtml")
	public String transRecordListUI(Integer transId, Model model){
		model.addAttribute("transId", transId);
		return "record/t-list";
	}
	
	@RequestMapping("job/record/listUI.shtml")
	public String jobRecordListUI(){
		return "record/j-list";
	}
	
	/**==========record end==========**/
	
	/**==========monitor start==========**/
	
	@RequestMapping("trans/monitor/listUI.shtml")
	public String transMonitorListUI(){
		return "monitor/t-list";
	}
	
	@RequestMapping("job/monitor/listUI.shtml")
	public String jobMonitorListUI(){
		return "monitor/j-list";
	}
	
	/**==========monitor start==========**/
}
