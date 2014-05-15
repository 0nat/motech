package org.motechproject.security.helper;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.motechproject.security.builder.SecurityRuleBuilder;
import org.motechproject.security.constants.HTTPMethod;
import org.motechproject.security.constants.Protocol;
import org.motechproject.security.constants.Scheme;
import org.motechproject.security.domain.MotechURLSecurityRule;
import org.motechproject.security.ex.SecurityConfigException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.Arrays;
import java.util.HashSet;

import static junit.framework.Assert.assertNotNull;
import static org.junit.Assert.assertEquals;
import static org.motechproject.security.constants.HTTPMethod.*;
import static org.motechproject.security.constants.Protocol.HTTP;
import static org.motechproject.security.constants.Scheme.USERNAME_PASSWORD;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = "classpath*:META-INF/motech/*.xml")
public class SecurityRuleBuilderIT {

    @Autowired
    private SecurityRuleBuilder securityBuilder;

    @Rule
    public ExpectedException configException = ExpectedException.none();

    @Test
    public void testShouldRequirePattern() {

        configException.expect(SecurityConfigException.class);
        configException.expectMessage(SecurityRuleBuilder.NO_PATTERN_EXCEPTION_MESSAGE);

        MotechURLSecurityRule securityRule = new MotechURLSecurityRule();
        SecurityFilterChain filterChain = securityBuilder.buildSecurityChain(securityRule, GET);
    }

    @Test
    public void testShouldRequireProtocol() {

        configException.expect(SecurityConfigException.class);
        configException.expectMessage(SecurityRuleBuilder.NO_PROTOCOL_EXCEPTION_MESSAGE);

        MotechURLSecurityRule securityRule = new MotechURLSecurityRule();
        securityRule.setPattern("pattern");
        SecurityFilterChain filterChain = securityBuilder.buildSecurityChain(securityRule, GET);
    }

    @Test
    public void testShouldRequireSupportedScheme() {
        configException.expect(SecurityConfigException.class);
        configException.expectMessage(SecurityRuleBuilder.NO_SUPPORTED_SCHEMES_EXCEPTION_MESSAGE);

        MotechURLSecurityRule securityRule = new MotechURLSecurityRule();
        securityRule.setPattern("pattern");
        securityRule.setProtocol(HTTP);

        SecurityFilterChain filterChain = securityBuilder.buildSecurityChain(securityRule, GET);
    }

    @Test
    public void testShouldRequireMethodsSupported() {

        configException.expect(SecurityConfigException.class);
        configException.expectMessage(SecurityRuleBuilder.NO_METHODS_REQUIRED_EXCEPTION_MESSAGE);

        MotechURLSecurityRule securityRule = new MotechURLSecurityRule();
        securityRule.setPattern("pattern");
        securityRule.setProtocol(HTTP);
        securityRule.setSupportedSchemes(Arrays.asList(USERNAME_PASSWORD));

        SecurityFilterChain filterChain = securityBuilder.buildSecurityChain(securityRule, GET);
    }

    @Test
    public void testMinimalRequirements() {

        MotechURLSecurityRule securityRule = new MotechURLSecurityRule();
        securityRule.setPattern("pattern");
        securityRule.setProtocol(HTTP);
        securityRule.setSupportedSchemes(Arrays.asList(USERNAME_PASSWORD));
        securityRule.setMethodsRequired(new HashSet<>(Arrays.asList(ANY)));

        SecurityFilterChain filterChain = securityBuilder.buildSecurityChain(securityRule, GET);

        assertNotNull(filterChain);
        assertEquals(10, filterChain.getFilters().size());
    }

}
