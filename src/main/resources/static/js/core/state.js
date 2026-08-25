/** 全局页面状态，只保存跨模块共享的数据 */

const $ = (id) => document.getElementById(id);
const medicineState = { page: 1, size: 20, totalPages: 0, editingId: null };
const diagnosisState = { page: 1, size: 20, totalPages: 0, editingId: null };
const facilityState = { page: 1, size: 20, totalPages: 0, editingId: null };
const diseaseState = { page: 1, size: 20, totalPages: 0, editingId: null };
const institutionState = { page: 1, size: 20, totalPages: 0, editingId: null };
const companyState = { page: 1, size: 20, totalPages: 0, editingId: null };
const personState = { page: 1, size: 20, totalPages: 0, editingId: null };
const auditState = { page: 1, size: 20, totalPages: 0 };
const treatmentState = { type: "capping", editing: null };
const approvalState = {
  institutionEditing: null,
  specialEditing: null,
  specialProjects: [],
};
const reimbursementState = {
  visitEditing: null,
  selectedVisit: null,
  prescriptionEditing: null,
  settlement: null,
  catalogItems: [],
};
const referenceState = {
  companies: [],
  institutions: [],
  people: [],
  diseases: [],
};
const bulkState = { modules: [] };
const tablePagerStates = new Map();
let currentRole = "",
  currentPageName = "dashboard",
  helpReturnPage = "dashboard",
  helpReturnScrollTop = 0;
