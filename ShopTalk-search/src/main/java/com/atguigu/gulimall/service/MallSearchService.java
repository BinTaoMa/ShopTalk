package com.atguigu.gulimall.service;

import com.atguigu.gulimall.vo.SearchParam;
import com.atguigu.gulimall.vo.SearchResult;

public interface MallSearchService {


	SearchResult search(SearchParam param);
}
