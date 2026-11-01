package com.supplychain.category;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.supplychain.IntegrationTest;
import com.supplychain.TestCatalog;
import com.supplychain.TestSessions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@IntegrationTest
@Import({TestSessions.class, TestCatalog.class})
class CategoryIntegrationTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private TestSessions sessions;

    @Autowired
    private TestCatalog catalog;

    @Test
    void anyoneCanListCategories() throws Exception {
        Category category = catalog.category();
        mvc.perform(get("/api/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == %d)].name".formatted(category.getId())).value(category.getName()));
    }

    @Test
    void onlyAdministratorsCanChangeCategories() throws Exception {
        String payload = "{\"name\":\"Audio\"}";
        long id = catalog.category().getId();

        mvc.perform(post("/api/categories").contentType(MediaType.APPLICATION_JSON).content(payload))
                .andExpect(status().isUnauthorized());
        mvc.perform(post("/api/categories")
                        .cookie(sessions.customer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isForbidden());
        mvc.perform(put("/api/categories/" + id)
                        .cookie(sessions.customer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isForbidden());
    }

    @Test
    void administratorCreatesAndRenames() throws Exception {
        long id = catalog.category().getId();
        String name = TestCatalog.unique("Audio");

        mvc.perform(post("/api/categories")
                        .cookie(sessions.admin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"%s\",\"description\":\"Sound\"}".formatted(name)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value(name));

        mvc.perform(put("/api/categories/" + id)
                        .cookie(sessions.admin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"%s\"}".formatted(name + "-renamed")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value(name + "-renamed"));
    }

    @Test
    void namesMustBeUnique() throws Exception {
        Category existing = catalog.category();

        mvc.perform(post("/api/categories")
                        .cookie(sessions.admin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"%s\"}".formatted(existing.getName())))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("A category with this name already exists."));
    }

    @Test
    void aBlankNameIsRefused() throws Exception {
        mvc.perform(post("/api/categories")
                        .cookie(sessions.admin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"  \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("name"));
    }

    @Test
    void renamingAMissingCategoryIsNotFound() throws Exception {
        mvc.perform(put("/api/categories/999999999")
                        .cookie(sessions.admin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"X\"}"))
                .andExpect(status().isNotFound());
    }
}
