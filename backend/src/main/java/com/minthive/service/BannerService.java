package com.minthive.service;

import com.minthive.entity.Banner;

import java.util.List;

public interface BannerService {

    List<Banner> listAll();

    List<Banner> listActive();

    Banner save(Banner banner);

    void deleteById(Long id);
}
