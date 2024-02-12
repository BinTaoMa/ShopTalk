package com.atguigu.gulimall.controller;

import com.atguigu.gulimall.service.MallSearchService;
import com.atguigu.gulimall.vo.SearchParam;
import com.atguigu.gulimall.vo.SearchResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;

@RestController
public class SearchController {
 
    @Autowired
    private MallSearchService mallSearchService;
 
    /**
     * 自动将页面提交过来的所有请求参数封装成我们指定的对象
     * @param param
     * @return
     */
    @GetMapping(value = "/list")
    public String listPage(SearchParam param, Model model, HttpServletRequest request) {
 
        param.set_queryString(request.getQueryString());
 
        //1、根据传递来的页面的查询参数，去es中检索商品
        SearchResult result = mallSearchService.search(param);
 
        model.addAttribute("result",result);
 
        return "list";
    }
 
}