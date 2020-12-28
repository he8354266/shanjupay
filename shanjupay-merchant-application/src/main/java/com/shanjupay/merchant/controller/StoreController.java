package com.shanjupay.merchant.controller;

import com.shanjupay.common.domain.BusinessException;
import com.shanjupay.common.domain.CommonErrorCode;
import com.shanjupay.common.domain.PageVO;
import com.shanjupay.common.util.QRCodeUtil;
import com.shanjupay.merchant.api.MerchantService;
import com.shanjupay.merchant.api.dto.MerchantDTO;
import com.shanjupay.merchant.api.dto.StoreDTO;
import com.shanjupay.transaction.api.TransactionService;
import com.shanjupay.transaction.api.dto.QRCodeDto;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.Reference;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

/**
 * @Title: project
 * @Package * @Description:     * @author CodingSir
 * @date 2020/12/2211:44
 */
@Api(value = "商户平台-门店管理", tags = "商户平台-门店管理", description = "商户平台-门店的增删改查")
@RestController
@Slf4j
public class StoreController {
    @Reference
    private MerchantService merchantService;
    @Value("${shanjupay.c2b.subject}")
    private String subject;
    @Value("${shanjupay.c2b.body}")
    private String body;


    @Reference
    private TransactionService transactionService;


    @ApiOperation("门店列表查询")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "pageNo", value = "页码", required = true, dataType = "int", paramType = "query"),
            @ApiImplicitParam(name = "pageSize", value = "每页记录数", required = true, dataType = "int", paramType = "query"),
            @ApiImplicitParam(name = "merchantId", value = "商户Id", required = true, dataType = "Long", paramType = "query")
    })
    @PostMapping("/my/stores/merchants/page")
    public PageVO<StoreDTO> queryStoreByPage(Integer pageNo, Integer pageSize, Long merchantId) {
        StoreDTO storeDTO = new StoreDTO();
        storeDTO.setMerchantId(merchantId);

        PageVO<StoreDTO> storeDTOS = merchantService.queryStoreByPage(storeDTO, pageSize, pageNo);
        return storeDTOS;
    }

    @ApiOperation("生成商户应用门店的二维码")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "appId", value = "商户应用Id", required = true, dataType = "String", paramType = "path"),
            @ApiImplicitParam(name = "storeId", value = "商户门店Id", required = true, dataType = "Long", paramType = "path"),
            @ApiImplicitParam(name = "merchantId", value = "商户Id", required = true, dataType = "Long", paramType = "query")
    })
    @GetMapping("/my/apps/{appId}/stores/{storeId}/app-store-qrcode")
    public String createCScanBStoreQRCode(@PathVariable("appId") String appId, @PathVariable("storeId") Long storeId, @RequestParam Long merchantId) {
        MerchantDTO merchantDTO = merchantService.queryMerchantById(merchantId);

        QRCodeDto qrCodeDto = new QRCodeDto();
        qrCodeDto.setMerchantId(merchantId);
        qrCodeDto.setStoreId(storeId);
        qrCodeDto.setAppId(appId);
        //标题.用商户名称替换 %s
        String subjectFormat = String.format(subject, merchantDTO.getMerchantName());
        qrCodeDto.setSubject(subjectFormat);

        //内容
        String bodyFormat = String.format(body, merchantDTO.getMerchantName());
        qrCodeDto.setBody(bodyFormat);

        //获取二维码的url
        String storeQRCodeURL = transactionService.createStoreQRCode(qrCodeDto);

        QRCodeUtil qrCodeUtil = new QRCodeUtil();
        //二维码图片base64编码
        String qrcode = null;
        try {
            qrcode = qrCodeUtil.createQRCode(storeQRCodeURL, 200, 200);

        } catch (IOException e) {
            e.printStackTrace();
            throw new BusinessException(CommonErrorCode.E_200007);
        }
        return qrcode;
    }
}
