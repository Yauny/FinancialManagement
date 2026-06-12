package com.fm.asset.repository.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fm.asset.repository.entity.AssetSnapshotDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * 资产快照 Mapper
 */
@Mapper
public interface AssetSnapshotMapper extends BaseMapper<AssetSnapshotDO> {
}
