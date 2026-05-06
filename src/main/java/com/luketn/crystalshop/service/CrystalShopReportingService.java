package com.luketn.crystalshop.service;

import com.luketn.crystalshop.domain.api.AnnualSalesReport;
import com.luketn.crystalshop.domain.api.MonthlyCustomerRetention;
import com.luketn.crystalshop.domain.api.ProductForecast;
import com.luketn.crystalshop.domain.api.ProductSalesInsight;
import com.luketn.crystalshop.domain.api.ReportTotals;
import com.luketn.crystalshop.domain.api.WeeklySalesTrend;
import com.luketn.crystalshop.http.ApiException;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.query.NativeQuery;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

public class CrystalShopReportingService {
    private final SessionFactory sessionFactory;

    public CrystalShopReportingService(SessionFactory sessionFactory) {
        this.sessionFactory = sessionFactory;
    }

    public AnnualSalesReport annualSalesReport(int year) {
        if (year < 2000 || year > 2100) {
            throw new ApiException(400, "year must be between 2000 and 2100");
        }
        return inTransaction(session -> {
            ReportWindow window = new ReportWindow(year);
            ReportTotals totals = totals(session, window);
            List<WeeklySalesTrend> weeklySalesTrends = weeklySalesTrends(session, window);
            List<MonthlyCustomerRetention> monthlyCustomerRetention = monthlyCustomerRetention(session, window);
            List<ProductSalesInsight> bestSellingProducts = bestSellingProducts(session, window);
            List<ProductForecast> forecasts = forecasts(session, window);
            return new AnnualSalesReport(
                    year,
                    year + 1,
                    totals,
                    weeklySalesTrends,
                    monthlyCustomerRetention,
                    bestSellingProducts,
                    forecasts,
                    recommendations(totals, monthlyCustomerRetention, bestSellingProducts, forecasts)
            );
        });
    }

    private ReportTotals totals(Session session, ReportWindow window) {
        String sql = """
                with report_lines as (
                    select
                        sl.sale_id,
                        s.customer_id,
                        sl.quantity,
                        sl.quantity * sl.unit_price as revenue,
                        sl.quantity * coalesce(c.wholesale_cost, c.retail_price * 0.55) as costs,
                        sl.quantity * (sl.unit_price - coalesce(c.wholesale_cost, c.retail_price * 0.55)) as profit
                    from sales s
                    join sale_lines sl on sl.sale_id = s.id
                    join crystals c on c.id = sl.crystal_id
                    where s.sold_at >= cast(:yearStart as timestamp)
                      and s.sold_at < cast(:nextYear as timestamp)
                ),
                annual_totals as (
                    select
                        coalesce(sum(revenue), 0) as revenue,
                        coalesce(sum(profit), 0) as profit,
                        coalesce(sum(costs), 0) as costs,
                        coalesce(sum(quantity), 0) as units_sold,
                        count(distinct sale_id) as sales_count,
                        count(distinct customer_id) as active_customers
                    from report_lines
                )
                select revenue, profit, costs, units_sold, sales_count, active_customers
                from annual_totals
                """;
        Object[] row = singleRow(query(session, sql, window));
        return new ReportTotals(
                money(row[0]),
                money(row[1]),
                money(row[2]),
                integer(row[3]),
                integer(row[4]),
                integer(row[5])
        );
    }

    private List<WeeklySalesTrend> weeklySalesTrends(Session session, ReportWindow window) {
        String sql = """
                with report_lines as (
                    select
                        date_trunc('week', s.sold_at)::date as week_start,
                        c.sku as crystal_sku,
                        c.name as crystal_name,
                        sl.quantity,
                        sl.quantity * sl.unit_price as revenue,
                        sl.quantity * (sl.unit_price - coalesce(c.wholesale_cost, c.retail_price * 0.55)) as profit
                    from sales s
                    join sale_lines sl on sl.sale_id = s.id
                    join crystals c on c.id = sl.crystal_id
                    where s.sold_at >= cast(:yearStart as timestamp)
                      and s.sold_at < cast(:nextYear as timestamp)
                ),
                weekly as (
                    select
                        week_start,
                        crystal_sku,
                        crystal_name,
                        sum(quantity) as units_sold,
                        sum(revenue) as revenue,
                        sum(profit) as profit
                    from report_lines
                    group by week_start, crystal_sku, crystal_name
                )
                select
                    to_char(week_start, 'YYYY-MM-DD') as week_start,
                    crystal_sku,
                    crystal_name,
                    units_sold,
                    revenue,
                    profit
                from weekly
                order by week_start, crystal_sku
                """;
        return query(session, sql, window).stream()
                .map(row -> new WeeklySalesTrend(
                        string(row[0]),
                        string(row[1]),
                        string(row[2]),
                        integer(row[3]),
                        money(row[4]),
                        money(row[5])
                ))
                .toList();
    }

    private List<MonthlyCustomerRetention> monthlyCustomerRetention(Session session, ReportWindow window) {
        String sql = """
                with months as (
                    select generate_series(
                        cast(:yearStart as date),
                        cast(:nextYear as date) - interval '1 month',
                        interval '1 month'
                    )::date as month_start
                ),
                monthly_active as (
                    select distinct
                        date_trunc('month', sold_at)::date as month_start,
                        customer_id
                    from sales
                    where sold_at >= cast(:yearStart as timestamp) - interval '1 month'
                      and sold_at < cast(:nextYear as timestamp)
                ),
                first_seen as (
                    select
                        customer_id,
                        min(date_trunc('month', sold_at)::date) as first_month
                    from sales
                    group by customer_id
                ),
                gained as (
                    select
                        m.month_start,
                        count(fs.customer_id) as customers_gained
                    from months m
                    left join first_seen fs on fs.first_month = m.month_start
                    group by m.month_start
                ),
                lost as (
                    select
                        m.month_start,
                        count(previous.customer_id) filter (where current_month.customer_id is null) as customers_lost
                    from months m
                    left join monthly_active previous
                        on previous.month_start = (m.month_start - interval '1 month')::date
                    left join monthly_active current_month
                        on current_month.month_start = m.month_start
                       and current_month.customer_id = previous.customer_id
                    group by m.month_start
                ),
                active as (
                    select
                        m.month_start,
                        count(current_month.customer_id) as active_customers
                    from months m
                    left join monthly_active current_month on current_month.month_start = m.month_start
                    group by m.month_start
                )
                select
                    to_char(m.month_start, 'YYYY-MM') as month,
                    coalesce(g.customers_gained, 0) as customers_gained,
                    coalesce(l.customers_lost, 0) as customers_lost,
                    coalesce(a.active_customers, 0) as active_customers
                from months m
                left join gained g on g.month_start = m.month_start
                left join lost l on l.month_start = m.month_start
                left join active a on a.month_start = m.month_start
                order by m.month_start
                """;
        return query(session, sql, window).stream()
                .map(row -> new MonthlyCustomerRetention(
                        string(row[0]),
                        integer(row[1]),
                        integer(row[2]),
                        integer(row[3])
                ))
                .toList();
    }

    private List<ProductSalesInsight> bestSellingProducts(Session session, ReportWindow window) {
        String sql = """
                with product_sales as (
                    select
                        c.sku as crystal_sku,
                        c.name as crystal_name,
                        sum(sl.quantity) as units_sold,
                        sum(sl.quantity * sl.unit_price) as revenue,
                        sum(sl.quantity * (sl.unit_price - coalesce(c.wholesale_cost, c.retail_price * 0.55))) as profit
                    from sales s
                    join sale_lines sl on sl.sale_id = s.id
                    join crystals c on c.id = sl.crystal_id
                    where s.sold_at >= cast(:yearStart as timestamp)
                      and s.sold_at < cast(:nextYear as timestamp)
                    group by c.sku, c.name
                ),
                ranked as (
                    select
                        crystal_sku,
                        crystal_name,
                        units_sold,
                        revenue,
                        profit,
                        profit / nullif(revenue, 0) as margin,
                        row_number() over (order by revenue desc, units_sold desc) as rank
                    from product_sales
                )
                select crystal_sku, crystal_name, units_sold, revenue, profit, coalesce(margin, 0)
                from ranked
                where rank <= 5
                order by rank
                """;
        return query(session, sql, window).stream()
                .map(row -> new ProductSalesInsight(
                        string(row[0]),
                        string(row[1]),
                        integer(row[2]),
                        money(row[3]),
                        money(row[4]),
                        decimal(row[5]).setScale(4, RoundingMode.HALF_UP)
                ))
                .toList();
    }

    private List<ProductForecast> forecasts(Session session, ReportWindow window) {
        String sql = """
                with report_lines as (
                    select
                        c.sku as crystal_sku,
                        c.name as crystal_name,
                        extract(month from s.sold_at) as sale_month,
                        sl.quantity,
                        sl.quantity * sl.unit_price as revenue
                    from sales s
                    join sale_lines sl on sl.sale_id = s.id
                    join crystals c on c.id = sl.crystal_id
                    where s.sold_at >= cast(:yearStart as timestamp)
                      and s.sold_at < cast(:nextYear as timestamp)
                ),
                annual as (
                    select
                        crystal_sku,
                        crystal_name,
                        sum(quantity) as units_sold,
                        sum(revenue) as revenue,
                        sum(case when sale_month <= 6 then revenue else 0 end) as first_half_revenue,
                        sum(case when sale_month > 6 then revenue else 0 end) as second_half_revenue
                    from report_lines
                    group by crystal_sku, crystal_name
                ),
                scored as (
                    select
                        crystal_sku,
                        crystal_name,
                        units_sold,
                        revenue,
                        greatest(
                            -0.15,
                            least(0.35, coalesce((second_half_revenue - first_half_revenue) / nullif(first_half_revenue, 0), 0.08))
                        ) as growth_rate
                    from annual
                )
                select
                    crystal_sku,
                    crystal_name,
                    round(revenue * (1 + growth_rate), 2) as projected_revenue,
                    ceil(units_sold * (1 + growth_rate))::int as projected_units,
                    round(growth_rate, 4) as growth_rate
                from scored
                order by projected_revenue desc
                limit 5
                """;
        return query(session, sql, window).stream()
                .map(row -> new ProductForecast(
                        string(row[0]),
                        string(row[1]),
                        money(row[2]),
                        integer(row[3]),
                        decimal(row[4]).setScale(4, RoundingMode.HALF_UP)
                ))
                .toList();
    }

    private List<String> recommendations(
            ReportTotals totals,
            List<MonthlyCustomerRetention> retention,
            List<ProductSalesInsight> bestSellingProducts,
            List<ProductForecast> forecasts
    ) {
        List<String> recommendations = new ArrayList<>();
        bestSellingProducts.stream().findFirst().ifPresent(product ->
                recommendations.add("Prioritise replenishment and front-of-store placement for "
                        + product.crystalSku() + " because it leads annual revenue."));
        forecasts.stream().findFirst().ifPresent(product ->
                recommendations.add("Increase 2026 purchasing cover for " + product.crystalSku()
                        + "; the forecast projects " + product.projectedUnits() + " units."));

        int gained = retention.stream().mapToInt(MonthlyCustomerRetention::customersGained).sum();
        int lost = retention.stream().mapToInt(MonthlyCustomerRetention::customersLost).sum();
        if (lost > gained) {
            recommendations.add("Run a win-back campaign for lapsed customers before peak gift-buying months.");
        } else {
            recommendations.add("Keep loyalty follow-ups active; customer gains are ahead of month-to-month losses.");
        }

        if (totals.revenue().compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal margin = totals.profit()
                    .divide(totals.revenue(), 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100));
            recommendations.add("Maintain the current cost discipline; annual gross margin is "
                    + margin.setScale(1, RoundingMode.HALF_UP) + "%.");
        }
        return recommendations;
    }

    @SuppressWarnings("unchecked")
    private List<Object[]> query(Session session, String sql, ReportWindow window) {
        NativeQuery<Object[]> query = session.createNativeQuery(sql, Object[].class);
        query.setParameter("yearStart", window.yearStart());
        query.setParameter("nextYear", window.nextYear());
        return query.getResultList();
    }

    private Object[] singleRow(List<Object[]> rows) {
        if (rows.isEmpty()) {
            return new Object[]{BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, 0, 0, 0};
        }
        return rows.getFirst();
    }

    private <T> T inTransaction(Function<Session, T> work) {
        try (Session session = sessionFactory.openSession()) {
            Transaction transaction = session.beginTransaction();
            try {
                T result = work.apply(session);
                transaction.commit();
                return result;
            } catch (RuntimeException e) {
                if (transaction.isActive()) {
                    transaction.rollback();
                }
                throw e;
            }
        }
    }

    private String string(Object value) {
        return value == null ? "" : value.toString();
    }

    private int integer(Object value) {
        if (value == null) {
            return 0;
        }
        if (value instanceof Number number) {
            return number.intValue();
        }
        return Integer.parseInt(value.toString());
    }

    private BigDecimal money(Object value) {
        return decimal(value).setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal decimal(Object value) {
        if (value == null) {
            return BigDecimal.ZERO;
        }
        if (value instanceof BigDecimal decimal) {
            return decimal;
        }
        if (value instanceof Number number) {
            return BigDecimal.valueOf(number.doubleValue());
        }
        return new BigDecimal(value.toString());
    }

    private record ReportWindow(int year) {
        String yearStart() {
            return year + "-01-01";
        }

        String nextYear() {
            return (year + 1) + "-01-01";
        }
    }
}
