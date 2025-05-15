package com.leonel.mulesoft.security.jwtfallback.internal;

import org.mule.runtime.extension.api.annotation.Extension;
import org.mule.runtime.extension.api.annotation.Configurations;
import org.mule.runtime.extension.api.annotation.dsl.xml.Xml;


/**
 * This is the main class of an extension, is the entry point from which configurations, connection providers, operations
 * and sources are going to be declared.
 */
@Xml(prefix = "multi-idp-jwt-validation")
@Extension(name = "Multi IdP JWT Validation")
@Configurations(MultiIdPJWTValidationConfiguration.class)
public class MultiIdPJWTValidationExtension {

}
