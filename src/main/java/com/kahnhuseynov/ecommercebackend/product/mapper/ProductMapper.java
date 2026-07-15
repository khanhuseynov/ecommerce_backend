package com.kahnhuseynov.ecommercebackend.product.mapper;

import com.kahnhuseynov.ecommercebackend.product.dto.ProductRequest;
import com.kahnhuseynov.ecommercebackend.product.dto.ProductResponse;
import com.kahnhuseynov.ecommercebackend.product.entity.Product;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface ProductMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Product toEntity(ProductRequest request);

    ProductResponse toResponse(Product product);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateEntity(ProductRequest request, @MappingTarget Product product);
}
