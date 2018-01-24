/*
 * Copyright 2014-2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package com.quancheng.saluki.gateway.portal.filter.controller;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import com.google.common.collect.Lists;
import com.quancheng.saluki.gateway.portal.common.BDException;
import com.quancheng.saluki.gateway.portal.common.BaseController;
import com.quancheng.saluki.gateway.portal.common.CommonResponse;
import com.quancheng.saluki.gateway.portal.common.Log;
import com.quancheng.saluki.gateway.portal.filter.dto.ZuulDto;
import com.quancheng.saluki.gateway.portal.filter.service.ProtobufService;
import com.quancheng.saluki.gateway.portal.filter.service.ZuulService;
import com.quancheng.saluki.gateway.portal.filter.vo.ZuulVo;
import com.quancheng.saluki.gateway.portal.system.domain.PageDO;
import com.quancheng.saluki.gateway.portal.utils.FileType;
import com.quancheng.saluki.gateway.portal.utils.Query;

/**
 * @author liushiming
 * @version ZuulController.java, v 0.0.1 2018年1月9日 上午11:19:14 liushiming
 */
@Controller
@RequestMapping("/filter/route")
public class FilterController extends BaseController {
  String prefix = "filter/route";

  @Autowired
  private ProtobufService protobufService;

  @Autowired
  private ZuulService zuulService;

  @RequiresPermissions("filter:route:route")
  @GetMapping()
  String route() {
    return prefix + "/route";
  }

  @Log("添加路由")
  @RequiresPermissions("filter:route:add")
  @GetMapping("/add")
  String add() {
    return prefix + "/add";
  }

  @Log("编辑路由")
  @RequiresPermissions("filter:route:edit")
  @GetMapping("/edit/{id}")
  String edit(@PathVariable("id") Long id, Model model) {
    ZuulDto zuulDto = zuulService.get(id);
    ZuulVo zuulVo = ZuulVo.buildZuulVo(zuulDto);
    model.addAttribute("zuul", zuulVo);
    return prefix + "/edit";
  }


  @RequiresPermissions("filter:route:route")
  @GetMapping("/list")
  @ResponseBody
  PageDO<ZuulVo> list(@RequestParam Map<String, Object> params) {
    Query query = new Query(params);
    PageDO<ZuulDto> pageDto = zuulService.queryList(query);
    PageDO<ZuulVo> pageVo = new PageDO<>();
    pageVo.setTotal(pageDto.getTotal());
    List<ZuulDto> zuulDtos = pageDto.getRows();
    List<ZuulVo> vos = Lists.newArrayListWithCapacity(zuulDtos.size());
    for (ZuulDto zuulDto : zuulDtos) {
      vos.add(ZuulVo.buildZuulVo(zuulDto));
    }
    pageVo.setRows(vos);
    return pageVo;
  }

  @Log("保存路由")
  @RequiresPermissions("filter:route:add")
  @PostMapping("/save")
  @ResponseBody()
  CommonResponse save(ZuulVo zuulVo,
      @RequestParam(name = "input", required = false) MultipartFile inputFile,
      @RequestParam(name = "output", required = false) MultipartFile outputFile,
      @RequestParam(name = "zipFile", required = false) MultipartFile zipFile) {
    try {
      // grpc路由
      if (zipFile != null) {
        InputStream directoryZipStream = zipFile.getInputStream();
        CommonResponse response = judgeFileType(directoryZipStream, "zip");
        if (response != null) {
          return response;
        } else {
          String serviceFileName = zuulVo.getServiceFileName();
          byte[] protoContext = protobufService.compileDirectoryProto(zipFile, serviceFileName);
          ZuulDto zuulDto = zuulVo.buildZuulDto();
          zuulDto.setProtoContext(protoContext);
          zuulService.save(zuulDto);
        }
      } else if (inputFile != null && outputFile != null) {
        InputStream inputStream = inputFile.getInputStream();
        InputStream outputStream = outputFile.getInputStream();
        CommonResponse responseInput = judgeFileType(inputStream, "proto");
        CommonResponse responseOutput = judgeFileType(outputStream, "proto");
        if (responseInput != null) {
          return responseInput;
        } else if (responseOutput != null) {
          return responseOutput;
        } else {
          String fileNameInput = inputFile.getOriginalFilename();
          byte[] protoInput = protobufService.compileFileProto(inputFile, fileNameInput);
          String fileNameOutput = outputFile.getOriginalFilename();
          byte[] protoOutput = protobufService.compileFileProto(outputFile, fileNameOutput);
          ZuulDto zuulDto = zuulVo.buildZuulDto();
          zuulDto.setProtoReq(protoInput);
          zuulDto.setProtoRep(protoOutput);
          zuulService.save(zuulDto);
        }
      } // rest路由
      else {
        ZuulDto zuulDto = zuulVo.buildZuulDto();
        zuulService.save(zuulDto);
      }
    } catch (IOException e) {
      throw new BDException("保存路由失败", e);
    }
    return CommonResponse.ok();
  }

  private CommonResponse judgeFileType(InputStream inpustream, String type) throws IOException {
    String fileType = FileType.calculateFileHexString(inpustream);
    if (!type.equals(fileType)) {
      return CommonResponse.error(1, "只能上传" + type + "类型文件");
    } else {
      return null;
    }
  }


  @Log("更新路由")
  @RequiresPermissions("filter:route:edit")
  @PostMapping("/update")
  @ResponseBody()
  CommonResponse update(ZuulVo zuulVo,
      @RequestParam(name = "input", required = false) MultipartFile inputFile,
      @RequestParam(name = "output", required = false) MultipartFile outputFile,
      @RequestParam(name = "zipFile", required = false) MultipartFile zipFile) {
    try {
      // grpc路由
      if (zipFile != null) {
        InputStream directoryZipStream = zipFile.getInputStream();
        CommonResponse response = judgeFileType(directoryZipStream, "zip");
        if (response != null) {
          return response;
        } else {
          String serviceFileName = zuulVo.getServiceFileName();
          byte[] protoContext = protobufService.compileDirectoryProto(zipFile, serviceFileName);
          ZuulDto zuulDto = zuulVo.buildZuulDto();
          zuulDto.setProtoContext(protoContext);
          zuulService.update(zuulDto);
        }
      } else if (inputFile != null && outputFile != null) {
        InputStream inputStream = inputFile.getInputStream();
        InputStream outputStream = outputFile.getInputStream();
        CommonResponse responseInput = judgeFileType(inputStream, "proto");
        CommonResponse responseOutput = judgeFileType(outputStream, "proto");
        if (responseInput != null) {
          return responseInput;
        } else if (responseOutput != null) {
          return responseOutput;
        } else {
          String fileNameInput = inputFile.getOriginalFilename();
          byte[] protoInput = protobufService.compileFileProto(inputFile, fileNameInput);
          String fileNameOutput = outputFile.getOriginalFilename();
          byte[] protoOutput = protobufService.compileFileProto(outputFile, fileNameOutput);
          ZuulDto zuulDto = zuulVo.buildZuulDto();
          zuulDto.setProtoReq(protoInput);
          zuulDto.setProtoRep(protoOutput);
          zuulService.update(zuulDto);
        }
      } // rest路由
      else {
        ZuulDto zuulDto = zuulVo.buildZuulDto();
        zuulService.update(zuulDto);
      }
    } catch (IOException e) {
      throw new BDException("保存路由失败", e);
    }
    return CommonResponse.ok();
  }

  @Log("删除路由")
  @RequiresPermissions("filter:route:remove")
  @PostMapping("/remove")
  @ResponseBody()
  CommonResponse save(Long id) {
    if (zuulService.remove(id) > 0) {
      return CommonResponse.ok();
    } else {
      return CommonResponse.error(1, "删除失败");
    }
  }

  @RequiresPermissions("filter:route:batchRemove")
  @Log("批量删除路由")
  @PostMapping("/batchRemove")
  @ResponseBody
  CommonResponse batchRemove(@RequestParam("ids[]") Long[] ids) {
    int response = zuulService.batchRemove(ids);
    if (response > 0) {
      return CommonResponse.ok();
    }
    return CommonResponse.error();
  }
}
