package com.wonderpush.sdk.segmentation.parser.datasource;

import androidx.annotation.Nullable;

import com.wonderpush.sdk.segmentation.parser.DataSource;
import com.wonderpush.sdk.segmentation.parser.DataSourceVisitor;

public class LastActivityDateSource extends DataSource {

    public LastActivityDateSource(@Nullable InstallationSource parent) {
        super(parent);
    }

    @Override
    public String getName() {
        return "lastActivityDate";
    }

    @Override
    public <T> T accept(DataSourceVisitor<T> visitor) {
        return visitor.visitLastActivityDateSource(this);
    }

}
