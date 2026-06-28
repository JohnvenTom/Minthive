package com.minthive.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.minthive.entity.Banner;
import com.minthive.mapper.BannerMapper;
import com.minthive.service.BannerService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@RequiredArgsConstructor
@Service
public class BannerServiceImpl implements BannerService {

    private final BannerMapper bannerMapper;

    @Override
    public List<Banner> listAll() {
        return bannerMapper.selectList(
                new LambdaQueryWrapper<Banner>()
                        .orderByAsc(Banner::getSort)
        );
    }

    @Override
    public List<Banner> listActive() {
        return bannerMapper.selectList(
                new LambdaQueryWrapper<Banner>()
                        .eq(Banner::getStatus, "ACTIVE")
                        .orderByAsc(Banner::getSort)
        );
    }

    @Override
    public Banner save(Banner banner) {
        if (banner.getId() != null) {
            bannerMapper.updateById(banner);
            return banner;
        }
        bannerMapper.insert(banner);
        return banner;
    }

    @Override
    public void deleteById(Long id) {
        bannerMapper.deleteById(id);
    }
}
