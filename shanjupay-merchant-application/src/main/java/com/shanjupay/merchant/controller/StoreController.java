package com.shanjupay.merchant.controller;

import com.shanjupay.common.domain.PageVO;
import com.shanjupay.merchant.api.MerchantService;
import com.shanjupay.merchant.api.dto.StoreDTO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.Reference;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

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

    @ApiOperation("门店列表查询")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "pageNo", value = "页码", required = true, dataType = "int", paramType = "query"),
            @ApiImplicitParam(name = "pageSize", value = "每页记录数", required = true, dataType = "int", paramType = "query"),
            @ApiImplicitParam(name = "merchantId", value = "商户Id", required = true, dataType = "Long", paramType = "query")
    })
    @PostMapping("/my/stores/merchants/page")
    public PageVO<StoreDTO> queryStoreByPage(Integer pageNo,  Integer pageSize,  Long merchantId) {
        StoreDTO storeDTO = new StoreDTO();
        storeDTO.setMerchantId(merchantId);

        PageVO<StoreDTO> storeDTOS = merchantService.queryStoreByPage(storeDTO, pageSize, pageNo);
        return storeDTOS;
    }
}
