package com.luketn.crystalshop;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.options.AriaRole;
import com.microsoft.playwright.options.SelectOption;
import com.luketn.crystalshop.persistence.HibernateSupport;
import org.hibernate.SessionFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.regex.Pattern;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

@Testcontainers
class CrystalShopMongoPlaywrightTest {
    @Container
    static final MongoDBContainer mongo = new MongoDBContainer(MongoTestSupport.MONGO_IMAGE);

    static CrystalShopApplication app;
    static Playwright playwright;
    static Browser browser;

    @BeforeAll
    static void startApplication() {
        try (SessionFactory sessionFactory = HibernateSupport.createSessionFactory(
                MongoTestSupport.mongoConfig(mongo, "none", 0)
        )) {
            new SampleDataImporter(sessionFactory).importSampleData();
        }

        app = CrystalShopApplication.start(MongoTestSupport.mongoConfig(mongo, "none", 0));

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
    void guiDrivesAllResourceFlowsThroughTheMongoApi() {
        try (BrowserContext context = browser.newContext(new Browser.NewContextOptions().setViewportSize(1400, 950))) {
            Page page = context.newPage();
            page.setDefaultTimeout(8_000);
            page.navigate(app.baseUri().toString());

            assertThat(page).hasTitle(Pattern.compile("Crystal Shop"));
            assertThat(page.getByRole(AriaRole.HEADING, options("Crystal Shop"))).isVisible();
            clickButton(page, "Refresh");
            assertRecordCount(page, "Crystals", "8 records");
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
            assertTableContains(page, "SEL-003 x3");
            exerciseReportFlow(page);
        }
    }

    private void exerciseCrystalFlow(Page page) {
        clickTab(page, "Crystals");
        assertRecordCount(page, "Crystals", "8 records");
        clickButton(page, "New");
        fill(page, "SKU", "FLU-888");
        fill(page, "Name", "Fluorite Octahedron");
        fill(page, "Family", "Halide");
        fill(page, "Color", "Green");
        fill(page, "Origin", "China");
        fill(page, "Retail Price", "22.40");
        fill(page, "Wholesale Cost", "9.25");
        clickButton(page, "Save");
        assertStatus(page, "Crystal saved.");
        assertRecordCount(page, "Crystals", "9 records");
        assertThat(page.getByText("Fluorite Octahedron")).isVisible();

        fill(page, "Color", "Green and purple");
        fill(page, "Retail Price", "24.10");
        fill(page, "Wholesale Cost", "10.05");
        clickButton(page, "Save");
        assertStatus(page, "Crystal saved.");
        assertThat(rowWithText(page, "FLU-888")).containsText("Green and purple");

        page.onceDialog(dialog -> dialog.accept());
        page.locator("#deleteButton").click();
        assertStatus(page, "Crystal deleted.");
        assertRecordCount(page, "Crystals", "8 records");
        assertThat(page.getByText("Fluorite Octahedron")).not().isVisible();
    }

    private void exerciseCustomerFlow(Page page) {
        clickTab(page, "Customers");
        assertRecordCount(page, "Customers", "10 records");
        clickButton(page, "New");
        fill(page, "Name", "Lina Torres");
        fill(page, "Email", "lina.torres@example.com");
        fill(page, "Loyalty Tier", "BRONZE");
        clickButton(page, "Save");
        assertStatus(page, "Customer saved.");
        assertRecordCount(page, "Customers", "11 records");

        fill(page, "Loyalty Tier", "GOLD");
        clickButton(page, "Save");
        assertStatus(page, "Customer saved.");
        assertThat(rowWithText(page, "lina.torres@example.com")).containsText("GOLD");

        clickButton(page, "Clear");
        assertThat(page.getByRole(AriaRole.HEADING, options("New Customer"))).isVisible();
        rowWithText(page, "lina.torres@example.com").click();
        page.onceDialog(dialog -> dialog.accept());
        page.locator("#deleteButton").click();
        assertStatus(page, "Customer deleted.");
        assertRecordCount(page, "Customers", "10 records");
        assertThat(page.getByText("lina.torres@example.com")).not().isVisible();
    }

    private void exerciseStoreFlow(Page page) {
        clickTab(page, "Stores");
        assertRecordCount(page, "Stores", "3 records");
        clickButton(page, "New");
        fill(page, "Code", "PER-GEM");
        fill(page, "Name", "Gem Hall");
        fill(page, "City", "Perth");
        fill(page, "Address", "9 Hay Street, Perth WA");
        clickButton(page, "Save");
        assertStatus(page, "Store saved.");
        assertRecordCount(page, "Stores", "4 records");

        fill(page, "City", "Fremantle");
        clickButton(page, "Save");
        assertStatus(page, "Store saved.");
        assertThat(page.getByText("Fremantle")).isVisible();

        page.onceDialog(dialog -> dialog.accept());
        page.locator("#deleteButton").click();
        assertStatus(page, "Store deleted.");
        assertRecordCount(page, "Stores", "3 records");
        assertThat(page.getByText("PER-GEM")).not().isVisible();
    }

    private void exerciseInventoryFlow(Page page) {
        clickTab(page, "Inventory");
        assertRecordCount(page, "Inventory", "18 records");
        clickButton(page, "New");
        select(page, "Store", "SYD-DAWN - Dawnlight Crystals");
        select(page, "Crystal", "LAB-004 - Labradorite Palm Stone");
        fill(page, "Quantity", "3");
        fill(page, "Shelf Location", "Z9");
        clickButton(page, "Save");
        assertStatus(page, "Inventory Item saved.");
        assertRecordCount(page, "Inventory", "19 records");
        assertThat(page.getByText("Z9")).isVisible();

        fill(page, "Quantity", "4");
        fill(page, "Shelf Location", "Z10");
        clickButton(page, "Save");
        assertStatus(page, "Inventory Item saved.");
        assertThat(page.getByText("Z10")).isVisible();

        page.onceDialog(dialog -> dialog.accept());
        page.locator("#deleteButton").click();
        assertStatus(page, "Inventory Item deleted.");
        assertRecordCount(page, "Inventory", "18 records");
        assertThat(page.getByText("Z10")).not().isVisible();
    }

    private void exerciseSaleFlow(Page page) {
        clickTab(page, "Sales");
        assertRecordCount(page, "Sales", "39 records");

        rowWithText(page, "SEL-003 x2").click();
        assertThat(page.locator(".sale-line-row")).hasCount(2);
        selectInLine(page, 0, "Crystal", "AME-001 - Amethyst Cluster");
        selectInLine(page, 1, "Crystal", "SEL-003 - Selenite Wand");
        fillInLine(page, 1, "Quantity", "3");
        clickButton(page, "Save");
        assertStatus(page, "Sale saved.");
        assertTableContains(page, "AME-001 x1, SEL-003 x3");
        assertThat(page.getByText("102.00")).isVisible();

        clickButton(page, "New");
        select(page, "Store", "SYD-DAWN - Dawnlight Crystals");
        select(page, "Customer", "Mira Chen - mira.chen@example.com");
        fill(page, "Sold At", "2026-04-24T11:15");
        selectInLine(page, 0, "Crystal", "AME-001 - Amethyst Cluster");
        fillInLine(page, 0, "Quantity", "1");
        fillInLine(page, 0, "Unit Price", "48.00");
        clickButton(page, "Add Line");
        selectInLine(page, 1, "Crystal", "SEL-003 - Selenite Wand");
        fillInLine(page, 1, "Quantity", "2");
        fillInLine(page, 1, "Unit Price", "18.00");
        clickButton(page, "Save");
        assertStatus(page, "Sale saved.");
        assertRecordCount(page, "Sales", "40 records");
        assertThat(page.getByText("2026-04-24T11:15")).isVisible();
        assertTableContains(page, "AME-001 x1, SEL-003 x2");

        fill(page, "Sold At", "2026-04-24T12:45");
        page.locator(".sale-line-row").nth(1).locator(".sale-line-remove").click();
        assertThat(page.locator(".sale-line-row")).hasCount(1);
        fillInLine(page, 0, "Quantity", "2");
        fillInLine(page, 0, "Unit Price", "47.50");
        clickButton(page, "Save");
        assertStatus(page, "Sale saved.");
        assertThat(page.getByText("2026-04-24T12:45")).isVisible();
        assertThat(rowWithText(page, "2026-04-24T12:45")).containsText("95.00");

        page.onceDialog(dialog -> dialog.accept());
        page.locator("#deleteButton").click();
        assertStatus(page, "Sale deleted.");
        assertRecordCount(page, "Sales", "39 records");
        assertThat(page.getByText("2026-04-24T12:45")).not().isVisible();
    }

    private void exerciseReportFlow(Page page) {
        clickTab(page, "Reports");
        assertThat(page.getByRole(AriaRole.HEADING, options("Annual Sales Report"))).isVisible();
        assertThat(page.locator("#reportTotals")).containsText("Revenue");
        assertThat(page.locator("#reportTotals")).containsText("Profit");
        assertThat(page.locator("#reportTotals")).containsText("Costs");
        assertThat(page.locator("#weeklyChart svg")).isVisible();
        assertThat(page.locator("#retentionChart svg")).isVisible();
        assertThat(page.locator("#bestSellers")).containsText("SKU");
        assertThat(page.locator("#forecastTable")).containsText("2026 Revenue");
        assertThat(page.locator("#recommendations")).containsText("Prioritise");

        fill(page, "Year", "2025");
        clickButton(page, "Run Report");
        assertThat(page.locator("#reportStatus")).hasText("Report loaded for 2025.");
    }

    private void clickTab(Page page, String name) {
        page.locator(".tab", new Page.LocatorOptions().setHasText(name)).click();
        String heading = "Reports".equals(name) ? "Annual Sales Report" : name;
        assertThat(page.getByRole(AriaRole.HEADING, options(heading))).isVisible();
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

    private void fillInLine(Page page, int index, String label, String value) {
        saleLine(page, index).getByLabel(label).fill(value);
    }

    private void selectInLine(Page page, int index, String label, String optionLabel) {
        saleLine(page, index).getByLabel(label).selectOption(new SelectOption().setLabel(optionLabel));
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

    private Locator saleLine(Page page, int index) {
        return page.locator(".sale-line-row").nth(index);
    }

    private static Page.GetByRoleOptions options(String name) {
        return new Page.GetByRoleOptions().setName(name).setExact(true);
    }
}
