const resources = {
  crystals: {
    label: "Crystals",
    singular: "Crystal",
    endpoint: "/crystals",
    columns: ["id", "sku", "name", "family", "color", "origin", "retailPrice"],
    fields: [
      { key: "sku", label: "SKU", required: true },
      { key: "name", label: "Name", required: true },
      { key: "family", label: "Family", required: true },
      { key: "color", label: "Color", required: true },
      { key: "origin", label: "Origin", required: true },
      { key: "retailPrice", label: "Retail Price", type: "number", step: "0.01", required: true }
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
    columns: ["id", "soldAt", "storeCode", "customerEmail", "total"],
    fields: [
      { key: "storeId", label: "Store", type: "select", source: "stores", required: true },
      { key: "customerId", label: "Customer", type: "select", source: "customers", required: true },
      { key: "soldAt", label: "Sold At", type: "datetime-local", required: true },
      { key: "lineCrystalId", label: "Line Crystal", type: "select", source: "crystals", required: true },
      { key: "lineQuantity", label: "Line Quantity", type: "integer", min: "1", required: true },
      { key: "lineUnitPrice", label: "Line Unit Price", type: "number", step: "0.01", required: true }
    ]
  }
};

const state = {
  current: "crystals",
  selectedId: null,
  data: {}
};

const tabs = document.getElementById("tabs");
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

document.getElementById("refreshButton").addEventListener("click", refreshAll);
document.getElementById("newButton").addEventListener("click", clearForm);
document.getElementById("clearButton").addEventListener("click", clearForm);
deleteButton.addEventListener("click", deleteSelected);
editorForm.addEventListener("submit", saveForm);

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
}

async function refreshAll() {
  setStatus("Loading data...", "");
  try {
    const entries = await Promise.all(Object.entries(resources).map(async ([key, config]) => {
      const rows = await api(config.endpoint);
      return [key, rows];
    }));
    state.data = Object.fromEntries(entries);
    renderCurrent();
    setStatus("Data loaded.", "ok");
  } catch (error) {
    setStatus(error.message, "error");
  }
}

function renderCurrent() {
  const config = resources[state.current];
  const rows = state.data[state.current] || [];
  resourceTitle.textContent = config.label;
  recordCount.textContent = `${rows.length} ${rows.length === 1 ? "record" : "records"}`;
  renderTable(config, rows);
  renderForm(config, selectedRow());
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
      td.title = displayValue(row[column]);
      td.textContent = displayValue(row[column]);
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
  fields.replaceChildren(...config.fields.map(field => createField(field, row)));
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
  if (key === "lineCrystalId") {
    return row.lines && row.lines[0] ? row.lines[0].crystalId : "";
  }
  if (key === "lineQuantity") {
    return row.lines && row.lines[0] ? row.lines[0].quantity : "1";
  }
  if (key === "lineUnitPrice") {
    return row.lines && row.lines[0] ? row.lines[0].unitPrice : "";
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
      lines: [
        {
          crystalId: numberValue(form, "lineCrystalId"),
          quantity: numberValue(form, "lineQuantity"),
          unitPrice: decimalValue(form, "lineUnitPrice")
        }
      ]
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

function textValue(form, key) {
  const value = String(form.get(key) || "").trim();
  if (!value) {
    throw new Error(`${labelize(key)} is required.`);
  }
  return value;
}

function numberValue(form, key) {
  const value = textValue(form, key);
  const parsed = Number.parseInt(value, 10);
  if (Number.isNaN(parsed)) {
    throw new Error(`${labelize(key)} must be a number.`);
  }
  return parsed;
}

function decimalValue(form, key) {
  const value = textValue(form, key);
  const parsed = Number.parseFloat(value);
  if (Number.isNaN(parsed)) {
    throw new Error(`${labelize(key)} must be a decimal number.`);
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

function displayValue(value) {
  if (value === null || value === undefined) {
    return "";
  }
  if (Array.isArray(value)) {
    return `${value.length} lines`;
  }
  return String(value);
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
