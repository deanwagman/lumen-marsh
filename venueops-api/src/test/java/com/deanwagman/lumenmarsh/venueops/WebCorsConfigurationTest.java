package com.deanwagman.lumenmarsh.venueops;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class WebCorsConfigurationTest {

    private static final String FLUTTER_WEB_ORIGIN = "http://localhost:53521";
    private static final String SEED_HERO_PATH = "/media/attractions/mangrove-run/hero.webp";

    @Autowired
    private MockMvc mockMvc;

    @Test
    void mediaGetAllowsFlutterWebDevelopmentOrigin() throws Exception {
        mockMvc.perform(get(SEED_HERO_PATH).header("Origin", FLUTTER_WEB_ORIGIN))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", FLUTTER_WEB_ORIGIN));
    }

    @Test
    void mediaOptionsPreflightAllowsGetHeadOptions() throws Exception {
        mockMvc.perform(options(SEED_HERO_PATH)
                        .header("Origin", FLUTTER_WEB_ORIGIN)
                        .header("Access-Control-Request-Method", "GET"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", FLUTTER_WEB_ORIGIN))
                .andExpect(header().string("Access-Control-Allow-Methods", "GET,HEAD,OPTIONS"));
    }
}
