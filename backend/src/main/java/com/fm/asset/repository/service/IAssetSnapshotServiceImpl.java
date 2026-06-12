package com.fm.asset.repository.service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fm.asset.repository.entity.AssetSnapshotDO;
import com.fm.asset.repository.mapper.AssetSnapshotMapper;
import org.springframework.stereotype.Service;

/**
 * 资产快照数据访问服务实现
 */
@Service
public class IAssetSnapshotServiceImpl extends ServiceImpl<AssetSnapshotMapper, AssetSnapshotDO> implements IAssetSnapshotService {
}
