package com.luketn.crystalshop;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.options.AriaRole;
import com.microsoft.playwright.options.SelectOption;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.regex.Pattern;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

@Testcontainers
class CrystalShopPlaywrightTest {
    @Container
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(DockerImageName.parse("postgres:16-alpine"))
            .withDatabaseName("crystal_shop")
            .withUsername("crystal")
            .withPassword("crystal");

    static CrystalShopApplication app;
    static Playwright playwright;
    static Browser browser;

    @BeforeAll
    static void startApplication() throws Exception {
        app = CrystalShopApplication.start(new AppConfig(
                postgres.getJdbcUrl(),
                postgres.getUsername(),
                postgres.getPassword(),
                "create-drop",
                0
        ));
        seedSampleData();

        playwright = Playwright.create();
        browser = playwright.chromium().launch();
    }

    @AfterAll
    static void stopApplication() {
        if (browser != null) {
            browser.close();
        }
        if (playwright != null) {
            playwright.close();
        }
        if (app != null) {
            app.close();
        }
    }

    @Test
    void guiDrivesAllResourceFlowsThroughTheApi() {
        try (BrowserContext context = browser.newContext(new Browser.NewContextOptions().setViewportSize(1400, 950))) {
            Page page = context.newPage();
            page.setDefaultTimeout(8_000);
            page.navigate(app.baseUri().toString());

            assertThat(page).hasTitle(Pattern.compile("Crystal Shop"));
            assertThat(page.getByRole(AriaRole.HEADING, options("Crystal Shop"))).isVisible();
            clickButton(page, "Refresh");
            assertRecordCount(page, "Crystals", "4 records");
            assertThat(page.getByText("Amethyst Cluster")).isVisible();

            exerciseCrystalFlow(page);
            exerciseCustomerFlow(page);
            exerciseStoreFlow(page);
            exerciseInventoryFlow(page);
            exerciseSaleFlow(page);

            clickTab(page, "Crystals");
            assertTableContains(page, "Amethyst Cluster");
            clickTab(page, "Customers");
            assertTableContains(page, "Mira Chen");
            clickTab(page, "Stores");
            assertTableContains(page, "Dawnlight Crystals");
            clickTab(page, "Inventory");
            assertTableContains(page, "SYD-DAWN");
            clickTab(page, "Sales");
            assertTableContains(page, "mira.chen@example.com");
        }
    }

    private void exerciseCrystalFlow(Page page) {
        clickTab(page, "Crystals");
        assertRecordCount(page, "Crystals", "4 records");
        clickButton(page, "New");
        fill(page, "SKU", "FLU-888");
        fill(page, "Name", "Fluorite Octahedron");
        fill(page, "Family", "Halide");
        fill(page, "Color", "Green");
        fill(page, "Origin", "China");
        fill(page, "Retail Price", "22.40");
        clickButton(page, "Save");
        assertStatus(page, "Crystal saved.");
        assertRecordCount(page, "Crystals", "5 records");
        assertThat(page.getByText("Fluorite Octahedron")).isVisible();

        fill(page, "Color", "Green and purple");
        fill(page, "Retail Price", "24.10");
        clickButton(page, "Save");
        assertStatus(page, "Crystal saved.");
        assertThat(page.getByText("Green and purple")).isVisible();

        page.onceDialog(dialog -> dialog.accept());
        page.locator("#deleteButton").click();
        assertStatus(page, "Crystal deleted.");
        assertRecordCount(page, "Crystals", "4 records");
        assertThat(page.getByText("Fluorite Octahedron")).not().isVisible();
    }

    private void exerciseCustomerFlow(Page page) {
        clickTab(page, "Customers");
        assertRecordCount(page, "Customers", "3 records");
        clickButton(page, "New");
        fill(page, "Name", "Lina Torres");
        fill(page, "Email", "lina.torres@example.com");
        fill(page, "Loyalty Tier", "BRONZE");
        clickButton(page, "Save");
        assertStatus(page, "Customer saved.");
        assertRecordCount(page, "Customers", "4 records");

        fill(page, "Loyalty Tier", "GOLD");
        clickButton(page, "Save");
        assertStatus(page, "Customer saved.");
        assertThat(page.getByText("GOLD")).isVisible();

        clickButton(page, "Clear");
        assertThat(page.getByRole(AriaRole.HEADING, options("New Customer"))).isVisible();
        rowWithText(page, "lina.torres@example.com").click();
        page.onceDialog(dialog -> dialog.accept());
        page.locator("#deleteButton").click();
        assertStatus(page, "Customer deleted.");
        assertRecordCount(page, "Customers", "3 records");
        assertThat(page.getByText("lina.torres@example.com")).not().isVisible();
    }

    private void exerciseStoreFlow(Page page) {
        clickTab(page, "Stores");
        assertRecordCount(page, "Stores", "2 records");
        clickButton(page, "New");
        fill(page, "Code", "PER-GEM");
        fill(page, "Name", "Gem Hall");
        fill(page, "City", "Perth");
        fill(page, "Address", "9 Hay Street, Perth WA");
        clickButton(page, "Save");
        assertStatus(page, "Store saved.");
        assertRecordCount(page, "Stores", "3 records");

        fill(page, "City", "Fremantle");
        clickButton(page, "Save");
        assertStatus(page, "Store saved.");
        assertThat(page.getByText("Fremantle")).isVisible();

        page.onceDialog(dialog -> dialog.accept());
        page.locator("#deleteButton").click();
        assertStatus(page, "Store deleted.");
        assertRecordCount(page, "Stores", "2 records");
        assertThat(page.getByText("PER-GEM")).not().isVisible();
    }

    private void exerciseInventoryFlow(Page page) {
        clickTab(page, "Inventory");
        assertRecordCount(page, "Inventory", "5 records");
        clickButton(page, "New");
        select(page, "Store", "SYD-DAWN - Dawnlight Crystals");
        select(page, "Crystal", "LAB-004 - Labradorite Palm Stone");
        fill(page, "Quantity", "3");
        fill(page, "Shelf Location", "Z9");
        clickButton(page, "Save");
        assertStatus(page, "Inventory Item saved.");
        assertRecordCount(page, "Inventory", "6 records");
        assertThat(page.getByText("Z9")).isVisible();

        fill(page, "Quantity", "4");
        fill(page, "Shelf Location", "Z10");
        clickButton(page, "Save");
        assertStatus(page, "Inventory Item saved.");
        assertThat(page.getByText("Z10")).isVisible();

        page.onceDialog(dialog -> dialog.accept());
        page.locator("#deleteButton").click();
        assertStatus(page, "Inventory Item deleted.");
        assertRecordCount(page, "Inventory", "5 records");
        assertThat(page.getByText("Z10")).not().isVisible();
    }

    private void exerciseSaleFlow(Page page) {
        clickTab(page, "Sales");
        assertRecordCount(page, "Sales", "3 records");
        clickButton(page, "New");
        select(page, "Store", "SYD-DAWN - Dawnlight Crystals");
        select(page, "Customer", "Mira Chen - mira.chen@example.com");
        fill(page, "Sold At", "2026-04-24T11:15");
        select(page, "Line Crystal", "AME-001 - Amethyst Cluster");
        fill(page, "Line Quantity", "1");
        fill(page, "Line Unit Price", "48.00");
        clickButton(page, "Save");
        assertStatus(page, "Sale saved.");
        assertRecordCount(page, "Sales", "4 records");
        assertThat(page.getByText("2026-04-24T11:15")).isVisible();

        fill(page, "Sold At", "2026-04-24T12:45");
        fill(page, "Line Quantity", "2");
        fill(page, "Line Unit Price", "47.50");
        clickButton(page, "Save");
        assertStatus(page, "Sale saved.");
        assertThat(page.getByText("2026-04-24T12:45")).isVisible();
        assertThat(page.getByText("95.00")).isVisible();

        page.onceDialog(dialog -> dialog.accept());
        page.locator("#deleteButton").click();
        assertStatus(page, "Sale deleted.");
        assertRecordCount(page, "Sales", "3 records");
        assertThat(page.getByText("2026-04-24T12:45")).not().isVisible();
    }

    private static void seedSampleData() throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder(app.baseUri().resolve("/sample-data"))
                .timeout(Duration.ofSeconds(10))
                .POST(HttpRequest.BodyPublishers.noBody())
                .build();
        HttpResponse<String> response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, response.statusCode(), response.body());
    }

    private void clickTab(Page page, String name) {
        page.locator(".tab", new Page.LocatorOptions().setHasText(name)).click();
        assertThat(page.getByRole(AriaRole.HEADING, options(name))).isVisible();
    }

    private void clickButton(Page page, String name) {
        page.getByRole(AriaRole.BUTTON, options(name)).click();
    }

    private void fill(Page page, String label, String value) {
        page.getByLabel(label).fill(value);
    }

    private void select(Page page, String label, String optionLabel) {
        page.getByLabel(label).selectOption(new SelectOption().setLabel(optionLabel));
    }

    private void assertRecordCount(Page page, String tabName, String count) {
        assertThat(page.getByRole(AriaRole.HEADING, options(tabName))).isVisible();
        assertThat(page.locator("#recordCount")).hasText(count);
    }

    private void assertStatus(Page page, String message) {
        assertThat(page.locator("#status")).hasText(message);
    }

    private void assertTableContains(Page page, String text) {
        assertThat(page.locator("tbody")).containsText(text);
    }

    private Locator rowWithText(Page page, String text) {
        return page.locator("tbody tr", new Page.LocatorOptions().setHasText(text)).first();
    }

    private static Page.GetByRoleOptions options(String name) {
        return new Page.GetByRoleOptions().setName(name).setExact(true);
    }
}
