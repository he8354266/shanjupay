package com.shanjupay.merchant.convert;

import com.shanjupay.merchant.api.dto.MerchantDTO;
import com.shanjupay.merchant.vo.MerchantRegisterVO;
import javax.annotation.Generated;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2020-12-18T13:54:12+0800",
    comments = "version: 1.3.0.Final, compiler: javac, environment: Java 1.8.0_251 (Oracle Corporation)"
)
public class MerchantRegisterConvertImpl implements MerchantRegisterConvert {

    @Override
    public MerchantRegisterVO dto2vo(MerchantDTO merchantDTO) {
        if ( merchantDTO == null ) {
            return null;
        }

        MerchantRegisterVO merchantRegisterVO = new MerchantRegisterVO();

        merchantRegisterVO.setMobile( merchantDTO.getMobile() );
        merchantRegisterVO.setUsername( merchantDTO.getUsername() );

        return merchantRegisterVO;
    }

    @Override
    public MerchantDTO vo2dto(MerchantRegisterVO merchantRegisterVO) {
        if ( merchantRegisterVO == null ) {
            return null;
        }

        MerchantDTO merchantDTO = new MerchantDTO();

        merchantDTO.setUsername( merchantRegisterVO.getUsername() );
        merchantDTO.setMobile( merchantRegisterVO.getMobile() );

        return merchantDTO;
    }
}
