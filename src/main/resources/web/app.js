const resources = {
  crystals: {
    label: "Crystals",
    singular: "Crystal",
    endpoint: "/crystals",
    columns: ["id", "sku", "name", "family", "color", "origin", "retailPrice", "wholesaleCost"],
    fields: [
      { key: "sku", label: "SKU", required: true },
      { key: "name", label: "Name", required: true },
      { key: "family", label: "Family", required: true },
      { key: "color", label: "Color", required: true },
      { key: "origin", label: "Origin", required: true },
      { key: "retailPrice", label: "Retail Price", type: "number", step: "0.01", required: true },
      { key: "wholesaleCost", label: "Wholesale Cost", type: "number", step: "0.01", required: true }
    ]
  },
  customers: {
    label: "Customers",
    singular: "Customer",
    endpoint: "/customers",
    columns: ["id", "name", "email", "loyaltyTier"],
    fields: [
      { key: "name", label: "Name", required: true },
      { key: "email", label: "Email", type: "email", required: true },
      { key: "loyaltyTier", label: "Loyalty Tier", required: true }
    ]
  },
  stores: {
    label: "Stores",
    singular: "Store",
    endpoint: "/stores",
    columns: ["id", "code", "name", "city", "address"],
    fields: [
      { key: "code", label: "Code", required: true },
      { key: "name", label: "Name", required: true },
      { key: "city", label: "City", required: true },
      { key: "address", label: "Address", required: true }
    ]
  },
  inventory: {
    label: "Inventory",
    singular: "Inventory Item",
    endpoint: "/inventory",
    columns: ["id", "storeCode", "crystalSku", "quantity", "shelfLocation"],
    fields: [
      { key: "storeId", label: "Store", type: "select", source: "stores", required: true },
      { key: "crystalId", label: "Crystal", type: "select", source: "crystals", required: true },
      { key: "quantity", label: "Quantity", type: "integer", min: "0", required: true },
      { key: "shelfLocation", label: "Shelf Location", required: true }
    ]
  },
  sales: {
    label: "Sales",
    singular: "Sale",
    endpoint: "/sales",
    columns: ["id", "soldAt", "storeCode", "customerEmail", "lineSummary", "total"],
    fields: [
      { key: "storeId", label: "Store", type: "select", source: "stores", required: true },
      { key: "customerId", label: "Customer", type: "select", source: "customers", required: true },
      { key: "soldAt", label: "Sold At", type: "datetime-local", required: true }
    ]
  }
};

const state = {
  current: "crystals",
  selectedId: null,
  data: {},
  reportYear: 2025,
  report: null
};

const moneyColumns = new Set(["retailPrice", "wholesaleCost", "unitPrice", "lineTotal", "total"]);
const reportTabKey = "reports";
const chartColors = ["#176b5d", "#b76622", "#4c6f9f", "#7a4d8f", "#2f7f3d", "#9c3d58", "#6e6a2f", "#3e7378"];

const tabs = document.getElementById("tabs");
const crudWorkspace = document.getElementById("crudWorkspace");
const reportWorkspace = document.getElementById("reportWorkspace");
const tableHead = document.getElementById("tableHead");
const tableBody = document.getElementById("tableBody");
const resourceTitle = document.getElementById("resourceTitle");
const recordCount = document.getElementById("recordCount");
const formTitle = document.getElementById("formTitle");
const recordMeta = document.getElementById("recordMeta");
const fields = document.getElementById("fields");
const editorForm = document.getElementById("editorForm");
const deleteButton = document.getElementById("deleteButton");
const statusBox = document.getElementById("status");
const reportForm = document.getElementById("reportForm");
const reportYear = document.getElementById("reportYear");
const reportMeta = document.getElementById("reportMeta");
const reportTotals = document.getElementById("reportTotals");
const weeklyChart = document.getElementById("weeklyChart");
const retentionChart = document.getElementById("retentionChart");
const bestSellers = document.getElementById("bestSellers");
const forecastTable = document.getElementById("forecastTable");
const recommendations = document.getElementById("recommendations");
const reportStatus = document.getElementById("reportStatus");

document.getElementById("refreshButton").addEventListener("click", refreshAll);
document.getElementById("newButton").addEventListener("click", clearForm);
document.getElementById("clearButton").addEventListener("click", clearForm);
deleteButton.addEventListener("click", deleteSelected);
editorForm.addEventListener("submit", saveForm);
reportForm.addEventListener("submit", runReport);

renderTabs();
refreshAll();

function renderTabs() {
  tabs.replaceChildren();
  Object.entries(resources).forEach(([key, config]) => {
    const button = document.createElement("button");
    button.type = "button";
    button.className = key === state.current ? "tab active" : "tab";
    button.textContent = config.label;
    button.addEventListener("click", () => {
      state.current = key;
      state.selectedId = null;
      renderTabs();
      renderCurrent();
    });
    tabs.append(button);
  });

  const reportButton = document.createElement("button");
  reportButton.type = "button";
  reportButton.className = state.current === reportTabKey ? "tab active" : "tab";
  reportButton.textContent = "Reports";
  reportButton.addEventListener("click", () => {
    state.current = reportTabKey;
    state.selectedId = null;
    renderTabs();
    renderCurrent();
  });
  tabs.append(reportButton);
}

async function refreshAll() {
  setStatus("Loading data...", "");
  setReportStatus("Loading report...", "");
  try {
    const [entries, report] = await Promise.all([
      Promise.all(Object.entries(resources).map(async ([key, config]) => {
        const rows = await api(config.endpoint);
        return [key, rows];
      })),
      fetchReport()
    ]);
    state.data = Object.fromEntries(entries);
    state.report = report;
    renderCurrent();
    setStatus("Data loaded.", "ok");
    setReportStatus(`Report loaded for ${state.reportYear}.`, "ok");
  } catch (error) {
    setStatus(error.message, "error");
    setReportStatus(error.message, "error");
  }
}

function renderCurrent() {
  if (state.current === reportTabKey) {
    crudWorkspace.hidden = true;
    reportWorkspace.hidden = false;
    renderReport();
    return;
  }

  crudWorkspace.hidden = false;
  reportWorkspace.hidden = true;
  const config = resources[state.current];
  const rows = state.data[state.current] || [];
  resourceTitle.textContent = config.label;
  recordCount.textContent = `${rows.length} ${rows.length === 1 ? "record" : "records"}`;
  renderTable(config, rows);
  renderForm(config, selectedRow());
}

async function runReport(event) {
  event.preventDefault();
  state.reportYear = Number.parseInt(reportYear.value, 10);
  setReportStatus("Loading report...", "");
  try {
    state.report = await fetchReport();
    renderReport();
    setReportStatus(`Report loaded for ${state.reportYear}.`, "ok");
  } catch (error) {
    setReportStatus(error.message, "error");
  }
}

async function fetchReport() {
  return api(`/reports/annual-sales?year=${encodeURIComponent(state.reportYear)}`);
}

function renderTable(config, rows) {
  const actionHeader = document.createElement("th");
  actionHeader.textContent = "";

  const headRow = document.createElement("tr");
  config.columns.forEach(column => {
    const th = document.createElement("th");
    th.textContent = labelize(column);
    headRow.append(th);
  });
  headRow.append(actionHeader);
  tableHead.replaceChildren(headRow);

  if (rows.length === 0) {
    const emptyRow = document.createElement("tr");
    const emptyCell = document.createElement("td");
    emptyCell.className = "empty";
    emptyCell.colSpan = config.columns.length + 1;
    emptyCell.textContent = "No records";
    emptyRow.append(emptyCell);
    tableBody.replaceChildren(emptyRow);
    return;
  }

  tableBody.replaceChildren(...rows.map(row => {
    const tr = document.createElement("tr");
    if (row.id === state.selectedId) {
      tr.classList.add("selected");
    }
    tr.addEventListener("click", () => selectRow(row.id));
    config.columns.forEach(column => {
      const td = document.createElement("td");
      td.title = displayValue(row[column], column);
      td.textContent = displayValue(row[column], column);
      tr.append(td);
    });

    const actions = document.createElement("td");
    const actionWrap = document.createElement("div");
    actionWrap.className = "row-actions";
    const edit = document.createElement("button");
    edit.type = "button";
    edit.className = "button secondary";
    edit.textContent = "Edit";
    edit.addEventListener("click", event => {
      event.stopPropagation();
      selectRow(row.id);
    });
    const remove = document.createElement("button");
    remove.type = "button";
    remove.className = "button danger";
    remove.textContent = "Delete";
    remove.addEventListener("click", event => {
      event.stopPropagation();
      selectRow(row.id);
      deleteSelected();
    });
    actionWrap.append(edit, remove);
    actions.append(actionWrap);
    tr.append(actions);
    return tr;
  }));
}

function renderForm(config, row) {
  const controls = config.fields.map(field => createField(field, row));
  if (state.current === "sales") {
    controls.push(createSaleLinesEditor(row));
  }
  fields.replaceChildren(...controls);
  formTitle.textContent = row ? `Edit ${config.singular}` : `New ${config.singular}`;
  recordMeta.textContent = row ? `ID ${row.id}` : "Ready";
  deleteButton.disabled = !row;
}

function createField(field, row) {
  const wrap = document.createElement("div");
  wrap.className = "field";
  const id = `field-${field.key}`;

  const label = document.createElement("label");
  label.htmlFor = id;
  label.textContent = field.label;

  const input = field.type === "select" ? document.createElement("select") : document.createElement("input");
  input.id = id;
  input.name = field.key;
  input.required = Boolean(field.required);

  if (field.type === "select") {
    addOptions(input, field.source);
  } else {
    input.type = field.type === "integer" ? "number" : field.type || "text";
    if (field.step) {
      input.step = field.step;
    }
    if (field.min) {
      input.min = field.min;
    }
  }

  input.value = fieldValue(field.key, row);
  wrap.append(label, input);
  return wrap;
}

function addOptions(select, source) {
  const placeholder = document.createElement("option");
  placeholder.value = "";
  placeholder.textContent = "Select";
  select.append(placeholder);

  (state.data[source] || []).forEach(row => {
    const option = document.createElement("option");
    option.value = row.id;
    option.textContent = optionLabel(source, row);
    select.append(option);
  });
}

function optionLabel(source, row) {
  if (source === "crystals") {
    return `${row.sku} - ${row.name}`;
  }
  if (source === "stores") {
    return `${row.code} - ${row.name}`;
  }
  if (source === "customers") {
    return `${row.name} - ${row.email}`;
  }
  return String(row.id);
}

function fieldValue(key, row) {
  if (!row) {
    if (key === "soldAt") {
      return new Date().toISOString().slice(0, 16);
    }
    if (key === "lineQuantity") {
      return "1";
    }
    return "";
  }
  if (key === "soldAt") {
    return String(row.soldAt || "").slice(0, 16);
  }
  return row[key] ?? "";
}

function selectRow(id) {
  state.selectedId = id;
  renderCurrent();
}

function clearForm() {
  state.selectedId = null;
  renderCurrent();
}

async function saveForm(event) {
  event.preventDefault();
  const config = resources[state.current];
  try {
    const payload = formPayload(config);
    const isUpdate = Boolean(state.selectedId);
    const path = isUpdate ? `${config.endpoint}/${state.selectedId}` : config.endpoint;
    const row = await api(path, {
      method: isUpdate ? "PUT" : "POST",
      body: JSON.stringify(payload)
    });
    state.selectedId = row.id;
    await refreshAll();
    setStatus(`${config.singular} saved.`, "ok");
  } catch (error) {
    setStatus(error.message, "error");
  }
}

async function deleteSelected() {
  const config = resources[state.current];
  if (!state.selectedId) {
    return;
  }
  if (!window.confirm(`Delete ${config.singular} ${state.selectedId}?`)) {
    return;
  }
  try {
    await api(`${config.endpoint}/${state.selectedId}`, { method: "DELETE" });
    state.selectedId = null;
    await refreshAll();
    setStatus(`${config.singular} deleted.`, "ok");
  } catch (error) {
    setStatus(error.message, "error");
  }
}

function formPayload(config) {
  const form = new FormData(editorForm);
  if (state.current === "sales") {
    return {
      storeId: numberValue(form, "storeId"),
      customerId: numberValue(form, "customerId"),
      soldAt: textValue(form, "soldAt"),
      lines: saleLinePayload(form)
    };
  }

  const payload = {};
  config.fields.forEach(field => {
    if (field.type === "number") {
      payload[field.key] = decimalValue(form, field.key);
    } else if (field.type === "integer" || field.type === "select") {
      payload[field.key] = numberValue(form, field.key);
    } else {
      payload[field.key] = textValue(form, field.key);
    }
  });
  return payload;
}

function createSaleLinesEditor(row) {
  const wrap = document.createElement("div");
  wrap.className = "sale-lines";

  const header = document.createElement("div");
  header.className = "sale-lines-header";
  const title = document.createElement("h3");
  title.textContent = "Sale Lines";
  const add = document.createElement("button");
  add.type = "button";
  add.className = "button secondary";
  add.textContent = "Add Line";
  add.addEventListener("click", () => {
    wrap.querySelector(".sale-lines-list").append(createSaleLineRow());
  });
  header.append(title, add);

  const list = document.createElement("div");
  list.className = "sale-lines-list";
  const lines = row && Array.isArray(row.lines) && row.lines.length > 0 ? row.lines : [{}];
  lines.forEach(line => list.append(createSaleLineRow(line)));

  wrap.append(header, list);
  return wrap;
}

function createSaleLineRow(line = {}) {
  const row = document.createElement("div");
  row.className = "sale-line-row";

  const crystal = createSaleLineControl("Crystal", "select", "lineCrystalId", line.crystalId || "");
  const quantity = createSaleLineControl("Quantity", "integer", "lineQuantity", line.quantity || "1");
  const unitPrice = createSaleLineControl("Unit Price", "number", "lineUnitPrice", line.unitPrice || "");

  const remove = document.createElement("button");
  remove.type = "button";
  remove.className = "button danger sale-line-remove";
  remove.textContent = "Remove";
  remove.addEventListener("click", () => {
    const list = row.parentElement;
    if (list && list.querySelectorAll(".sale-line-row").length > 1) {
      row.remove();
    } else {
      setStatus("A sale needs at least one line.", "error");
    }
  });

  row.append(crystal, quantity, unitPrice, remove);
  return row;
}

function createSaleLineControl(labelText, type, name, value) {
  const wrap = document.createElement("div");
  wrap.className = "field";

  const label = document.createElement("label");
  label.textContent = labelText;

  const input = type === "select" ? document.createElement("select") : document.createElement("input");
  input.name = name;
  input.required = true;
  input.setAttribute("aria-label", labelText);

  if (type === "select") {
    addOptions(input, "crystals");
  } else {
    input.type = "number";
    if (type === "integer") {
      input.min = "1";
      input.step = "1";
    } else {
      input.step = "0.01";
    }
  }

  input.value = value;
  wrap.append(label, input);
  return wrap;
}

function saleLinePayload(form) {
  const crystalIds = form.getAll("lineCrystalId");
  const quantities = form.getAll("lineQuantity");
  const unitPrices = form.getAll("lineUnitPrice");
  if (crystalIds.length === 0) {
    throw new Error("At least one sale line is required.");
  }
  return crystalIds.map((_, index) => ({
    crystalId: requiredNumber(crystalIds[index], `Line ${index + 1} Crystal`),
    quantity: requiredNumber(quantities[index], `Line ${index + 1} Quantity`),
    unitPrice: requiredDecimal(unitPrices[index], `Line ${index + 1} Unit Price`)
  }));
}

function renderReport() {
  reportYear.value = state.reportYear;
  const report = state.report;
  if (!report) {
    reportMeta.textContent = "No report loaded";
    reportTotals.replaceChildren();
    weeklyChart.textContent = "No report data";
    retentionChart.textContent = "No report data";
    bestSellers.replaceChildren();
    forecastTable.replaceChildren();
    recommendations.replaceChildren();
    return;
  }

  reportMeta.textContent = `${report.year} with ${report.forecastYear} projections`;
  renderReportTotals(report.totals);
  renderWeeklyChart(report.weeklySalesTrends || []);
  renderRetentionChart(report.monthlyCustomerRetention || []);
  renderBestSellers(report.bestSellingProducts || []);
  renderForecasts(report.forecasts || [], report.forecastYear);
  renderRecommendations(report.recommendations || []);
}

function renderReportTotals(totals) {
  const metrics = [
    ["Revenue", formatMoney(totals.revenue)],
    ["Profit", formatMoney(totals.profit)],
    ["Costs", formatMoney(totals.costs)],
    ["Units Sold", String(totals.unitsSold)],
    ["Sales", String(totals.salesCount)],
    ["Active Customers", String(totals.activeCustomers)]
  ];

  reportTotals.replaceChildren(...metrics.map(([label, value]) => {
    const item = document.createElement("div");
    item.className = "metric-card";
    const metricLabel = document.createElement("span");
    metricLabel.textContent = label;
    const metricValue = document.createElement("strong");
    metricValue.textContent = value;
    item.append(metricLabel, metricValue);
    return item;
  }));
}

function renderWeeklyChart(rows) {
  weeklyChart.replaceChildren();
  if (rows.length === 0) {
    weeklyChart.textContent = "No weekly sales for this year.";
    return;
  }

  const weeks = [...new Set(rows.map(row => row.weekStart))].sort();
  const crystals = [...new Map(rows.map(row => [row.crystalSku, row.crystalName])).entries()];
  const revenueByCrystal = new Map();
  rows.forEach(row => {
    const crystalRows = revenueByCrystal.get(row.crystalSku) || new Map();
    crystalRows.set(row.weekStart, Number(row.revenue || 0));
    revenueByCrystal.set(row.crystalSku, crystalRows);
  });

  const width = 960;
  const height = 330;
  const margin = { top: 22, right: 24, bottom: 48, left: 62 };
  const chartWidth = width - margin.left - margin.right;
  const chartHeight = height - margin.top - margin.bottom;
  const maxRevenue = Math.max(1, ...rows.map(row => Number(row.revenue || 0)));

  const svg = svgEl("svg", { viewBox: `0 0 ${width} ${height}`, role: "img" });
  svg.append(svgEl("line", {
    x1: margin.left,
    y1: margin.top + chartHeight,
    x2: margin.left + chartWidth,
    y2: margin.top + chartHeight,
    class: "chart-axis"
  }));
  svg.append(svgEl("line", {
    x1: margin.left,
    y1: margin.top,
    x2: margin.left,
    y2: margin.top + chartHeight,
    class: "chart-axis"
  }));

  const tickCount = 4;
  for (let tick = 0; tick <= tickCount; tick += 1) {
    const value = maxRevenue * tick / tickCount;
    const y = margin.top + chartHeight - (value / maxRevenue) * chartHeight;
    svg.append(svgEl("line", {
      x1: margin.left,
      y1: y,
      x2: margin.left + chartWidth,
      y2: y,
      class: "chart-grid"
    }));
    const label = svgEl("text", { x: margin.left - 8, y: y + 4, class: "chart-label", "text-anchor": "end" });
    label.textContent = compactMoney(value);
    svg.append(label);
  }

  weeks.forEach((week, index) => {
    if (index % Math.max(1, Math.ceil(weeks.length / 8)) !== 0 && index !== weeks.length - 1) {
      return;
    }
    const x = xForIndex(index, weeks.length, margin.left, chartWidth);
    const label = svgEl("text", {
      x,
      y: height - 18,
      class: "chart-label",
      "text-anchor": "middle"
    });
    label.textContent = week.slice(5);
    svg.append(label);
  });

  crystals.forEach(([sku], index) => {
    const color = chartColors[index % chartColors.length];
    const points = weeks.map((week, weekIndex) => {
      const revenue = revenueByCrystal.get(sku)?.get(week) || 0;
      const x = xForIndex(weekIndex, weeks.length, margin.left, chartWidth);
      const y = margin.top + chartHeight - (revenue / maxRevenue) * chartHeight;
      return `${x},${y}`;
    }).join(" ");
    svg.append(svgEl("polyline", {
      points,
      fill: "none",
      stroke: color,
      "stroke-width": 3,
      "stroke-linejoin": "round",
      "stroke-linecap": "round"
    }));
  });

  const legend = document.createElement("div");
  legend.className = "chart-legend";
  legend.replaceChildren(...crystals.map(([sku, name], index) => legendItem(sku, name, chartColors[index % chartColors.length])));
  weeklyChart.append(svg, legend);
}

function renderRetentionChart(rows) {
  retentionChart.replaceChildren();
  if (rows.length === 0) {
    retentionChart.textContent = "No retention data for this year.";
    return;
  }

  const width = 860;
  const height = 300;
  const margin = { top: 24, right: 20, bottom: 42, left: 44 };
  const chartWidth = width - margin.left - margin.right;
  const chartHeight = height - margin.top - margin.bottom;
  const maxCustomers = Math.max(1, ...rows.map(row => Number(row.customersGained || 0) + Number(row.customersLost || 0)));
  const barSlot = chartWidth / rows.length;
  const barWidth = Math.max(18, barSlot * 0.58);
  const baseY = margin.top + chartHeight;

  const svg = svgEl("svg", { viewBox: `0 0 ${width} ${height}`, role: "img" });
  svg.append(svgEl("line", {
    x1: margin.left,
    y1: baseY,
    x2: margin.left + chartWidth,
    y2: baseY,
    class: "chart-axis"
  }));

  rows.forEach((row, index) => {
    const gained = Number(row.customersGained || 0);
    const lost = Number(row.customersLost || 0);
    const gainedHeight = gained / maxCustomers * chartHeight;
    const lostHeight = lost / maxCustomers * chartHeight;
    const x = margin.left + index * barSlot + (barSlot - barWidth) / 2;
    const gainedY = baseY - gainedHeight;
    const lostY = gainedY - lostHeight;

    svg.append(svgEl("rect", {
      x,
      y: gainedY,
      width: barWidth,
      height: gainedHeight,
      class: "bar-gained"
    }));
    svg.append(svgEl("rect", {
      x,
      y: lostY,
      width: barWidth,
      height: lostHeight,
      class: "bar-lost"
    }));

    const label = svgEl("text", {
      x: x + barWidth / 2,
      y: height - 16,
      class: "chart-label",
      "text-anchor": "middle"
    });
    label.textContent = row.month.slice(5);
    svg.append(label);
  });

  const legend = document.createElement("div");
  legend.className = "chart-legend";
  legend.append(legendItem("Gained", "Customers first seen", "#2f8a4b"));
  legend.append(legendItem("Lost", "Inactive after previous month", "#a33b3b"));
  retentionChart.append(svg, legend);
}

function renderBestSellers(rows) {
  renderReportTable(bestSellers, [
    ["SKU", row => row.crystalSku],
    ["Product", row => row.crystalName],
    ["Units", row => row.unitsSold],
    ["Revenue", row => formatMoney(row.revenue)],
    ["Profit", row => formatMoney(row.profit)],
    ["Margin", row => formatPercent(row.margin)]
  ], rows);
}

function renderForecasts(rows, forecastYear) {
  renderReportTable(forecastTable, [
    ["SKU", row => row.crystalSku],
    ["Product", row => row.crystalName],
    [`${forecastYear} Units`, row => row.projectedUnits],
    [`${forecastYear} Revenue`, row => formatMoney(row.projectedRevenue)],
    ["Growth", row => formatPercent(row.growthRate)]
  ], rows);
}

function renderReportTable(container, columns, rows) {
  const table = document.createElement("table");
  const thead = document.createElement("thead");
  const headRow = document.createElement("tr");
  columns.forEach(([label]) => {
    const th = document.createElement("th");
    th.textContent = label;
    headRow.append(th);
  });
  thead.append(headRow);

  const tbody = document.createElement("tbody");
  if (rows.length === 0) {
    const tr = document.createElement("tr");
    const td = document.createElement("td");
    td.className = "empty";
    td.colSpan = columns.length;
    td.textContent = "No rows";
    tr.append(td);
    tbody.append(tr);
  } else {
    rows.forEach(row => {
      const tr = document.createElement("tr");
      columns.forEach(([, value]) => {
        const td = document.createElement("td");
        td.textContent = String(value(row));
        tr.append(td);
      });
      tbody.append(tr);
    });
  }

  table.append(thead, tbody);
  container.replaceChildren(table);
}

function renderRecommendations(items) {
  recommendations.replaceChildren(...items.map(item => {
    const li = document.createElement("li");
    li.textContent = item;
    return li;
  }));
}

function textValue(form, key) {
  const value = String(form.get(key) || "").trim();
  if (!value) {
    throw new Error(`${labelize(key)} is required.`);
  }
  return value;
}

function numberValue(form, key) {
  return requiredNumber(textValue(form, key), labelize(key));
}

function decimalValue(form, key) {
  return requiredDecimal(textValue(form, key), labelize(key));
}

function requiredNumber(value, label) {
  const parsed = Number.parseInt(value, 10);
  if (Number.isNaN(parsed)) {
    throw new Error(`${label} must be a number.`);
  }
  return parsed;
}

function requiredDecimal(value, label) {
  const parsed = Number.parseFloat(value);
  if (Number.isNaN(parsed)) {
    throw new Error(`${label} must be a decimal number.`);
  }
  return parsed;
}

function selectedRow() {
  if (!state.selectedId) {
    return null;
  }
  return (state.data[state.current] || []).find(row => row.id === state.selectedId) || null;
}

async function api(path, options = {}) {
  const response = await fetch(path, {
    headers: options.body === undefined ? undefined : { "Content-Type": "application/json" },
    ...options
  });
  const text = await response.text();
  const payload = text ? JSON.parse(text) : null;
  if (!response.ok) {
    throw new Error(payload && payload.error ? payload.error : `HTTP ${response.status}`);
  }
  return payload;
}

function displayValue(value, key) {
  if (value === null || value === undefined) {
    return "";
  }
  if (moneyColumns.has(key)) {
    const number = Number(value);
    if (Number.isFinite(number)) {
      return number.toFixed(2);
    }
  }
  if (key === "lineSummary" && Array.isArray(value)) {
    return value.join(", ");
  }
  if (Array.isArray(value)) {
    return `${value.length} lines`;
  }
  return String(value);
}

function formatMoney(value) {
  const number = Number(value || 0);
  if (!Number.isFinite(number)) {
    return "$0.00";
  }
  return `$${number.toFixed(2)}`;
}

function compactMoney(value) {
  const number = Number(value || 0);
  if (number >= 1000) {
    return `$${(number / 1000).toFixed(1)}k`;
  }
  return `$${number.toFixed(0)}`;
}

function formatPercent(value) {
  const number = Number(value || 0);
  if (!Number.isFinite(number)) {
    return "0.0%";
  }
  return `${(number * 100).toFixed(1)}%`;
}

function xForIndex(index, count, left, width) {
  if (count <= 1) {
    return left + width / 2;
  }
  return left + (index / (count - 1)) * width;
}

function svgEl(name, attrs = {}) {
  const element = document.createElementNS("http://www.w3.org/2000/svg", name);
  Object.entries(attrs).forEach(([key, value]) => element.setAttribute(key, value));
  return element;
}

function legendItem(label, title, color) {
  const item = document.createElement("span");
  item.className = "legend-item";
  item.title = title;

  const swatch = document.createElement("span");
  swatch.className = "legend-swatch";
  swatch.style.background = color;

  const text = document.createElement("span");
  text.textContent = label;
  item.append(swatch, text);
  return item;
}

function labelize(key) {
  return key
    .replace(/Id$/, " ID")
    .replace(/([A-Z])/g, " $1")
    .replace(/^./, char => char.toUpperCase());
}

function setStatus(message, type) {
  statusBox.textContent = message;
  statusBox.className = type ? `status ${type}` : "status";
}

function setReportStatus(message, type) {
  reportStatus.textContent = message;
  reportStatus.className = type ? `status ${type}` : "status";
}
