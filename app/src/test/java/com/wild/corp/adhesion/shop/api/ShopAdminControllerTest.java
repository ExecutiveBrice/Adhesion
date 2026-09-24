package com.wild.corp.adhesion.shop.api;

import com.wild.corp.adhesion.security.WebSecurityConfig;
import com.wild.corp.adhesion.security.jwt.AuthEntryPointJwt;
import com.wild.corp.adhesion.security.jwt.JwtUtils;
import com.wild.corp.adhesion.services.UserDetailsService;
import com.wild.corp.adhesion.shop.catalog.service.CatalogService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = ShopAdminController.class)
@ContextConfiguration(classes = {ShopAdminController.class, WebSecurityConfig.class, AuthEntryPointJwt.class})
@Import(ShopApiExceptionHandler.class)
class ShopAdminControllerTest {

    @Autowired private MockMvc mockMvc;
    @MockitoBean private CatalogService catalogService;
    @MockitoBean private UserDetailsService userDetailsService;
    @MockitoBean private JwtUtils jwtUtils;
    @MockitoBean private AuthenticationManager authenticationManager;

    @Test
    void rejectsOrdinaryUser() throws Exception {
        mockMvc.perform(get("/shop/admin/products").with(user("member@example.test").roles("USER")))
                .andExpect(status().isForbidden());
        verifyNoInteractions(catalogService);
    }

    @Test
    void allowsOnlyShopManager() throws Exception {
        given(catalogService.findAllProducts()).willReturn(List.of());

        mockMvc.perform(get("/shop/admin/products").with(user("manager@example.test").roles("RESPONSABLE_BOUTIQUE")))
                .andExpect(status().isOk());
    }
}
