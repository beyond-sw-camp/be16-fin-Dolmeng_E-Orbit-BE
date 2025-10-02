package com.Dolmeng_E.workspace.common.domain;

import org.hibernate.HibernateException;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.id.enhanced.SequenceStyleGenerator;
import org.hibernate.internal.util.config.ConfigurationHelper;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.type.Type;
import org.hibernate.type.spi.TypeConfiguration;

import java.io.Serializable;
import java.util.Properties;

// Custom ID Generator 클래스
public class StringPrefixedSequenceIdGenerator extends SequenceStyleGenerator {

    public static final String VALUE_PREFIX_PARAMETER = "valuePrefix";
    private String valuePrefix;

    @Override
    public Serializable generate(SharedSessionContractImplementor session, Object object) throws HibernateException {
        // valuePrefix(예: "ws")와 DB 시퀀스 값을 조합하여 ID 생성
        return valuePrefix + super.generate(session, object);
    }

    @Override
    public void configure(Type type, Properties params, ServiceRegistry serviceRegistry) {
        super.configure(new TypeConfiguration().getBasicTypeRegistry().getRegisteredType(Long.class), params, serviceRegistry);
        // Entity에서 @Parameter 로 넘겨준 valuePrefix 값을 읽어옴
        this.valuePrefix = ConfigurationHelper.getString(VALUE_PREFIX_PARAMETER, params);
    }
}
