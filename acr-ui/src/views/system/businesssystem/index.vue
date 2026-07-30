<template>
   <div class="app-container">
      <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch" label-width="68px">
         <el-form-item label="系统名称" prop="systemName">
            <el-input
               v-model="queryParams.systemName"
               placeholder="请输入系统名称"
               clearable
               style="width: 200px"
               @keyup.enter="handleQuery"
            />
         </el-form-item>
         <el-form-item label="系统编码" prop="systemCode">
            <el-input
               v-model="queryParams.systemCode"
               placeholder="请输入系统编码"
               clearable
               style="width: 200px"
               @keyup.enter="handleQuery"
            />
         </el-form-item>
         <el-form-item label="状态" prop="status">
            <el-select v-model="queryParams.status" placeholder="请选择状态" clearable style="width: 120px">
               <el-option label="正常" value="0" />
               <el-option label="停用" value="1" />
            </el-select>
         </el-form-item>
         <el-form-item>
            <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
            <el-button icon="Refresh" @click="resetQuery">重置</el-button>
         </el-form-item>
      </el-form>

      <el-row :gutter="10" class="mb8">
         <el-col :span="1.5">
            <el-button
               type="primary"
               plain
               icon="Plus"
               @click="handleAdd"
               v-hasPermi="['system:businesssystem:add']"
            >新增</el-button>
         </el-col>
         <right-toolbar v-model:showSearch="showSearch" @queryTable="getList"></right-toolbar>
      </el-row>

      <el-table v-loading="loading" :data="systemList">
         <el-table-column label="系统名称" align="center" prop="systemName" :show-overflow-tooltip="true" />
         <el-table-column label="系统编码" align="center" prop="systemCode" width="150" />
         <el-table-column label="所属部门" align="center" prop="deptName" :show-overflow-tooltip="true" />
         <el-table-column label="管理用户" align="center" prop="managerNames" :show-overflow-tooltip="true" />
         <el-table-column label="状态" align="center" prop="status" width="80">
            <template #default="scope">
               <dict-tag :options="sys_normal_disable" :value="scope.row.status" />
            </template>
         </el-table-column>
         <el-table-column label="备注" align="center" prop="remark" :show-overflow-tooltip="true" />
         <el-table-column label="操作" align="center" width="150" class-name="small-padding fixed-width">
            <template #default="scope">
               <el-button link type="primary" icon="Edit" @click="handleUpdate(scope.row)" v-hasPermi="['system:businesssystem:edit']">修改</el-button>
               <el-button link type="primary" icon="Delete" @click="handleDelete(scope.row)" v-hasPermi="['system:businesssystem:remove']">删除</el-button>
            </template>
         </el-table-column>
      </el-table>

      <pagination
         v-show="total > 0"
         :total="total"
         v-model:page="queryParams.pageNum"
         v-model:limit="queryParams.pageSize"
         @pagination="getList"
      />

      <!-- 添加或修改对话框 -->
      <el-dialog :title="title" v-model="open" width="600px" append-to-body>
         <el-form ref="systemRef" :model="form" :rules="rules" label-width="100px">
            <el-row :gutter="20">
               <el-col :span="12">
                  <el-form-item label="系统名称" prop="systemName">
                     <el-input v-model="form.systemName" placeholder="如: 理赔系统" />
                  </el-form-item>
               </el-col>
               <el-col :span="12">
                  <el-form-item label="系统编码" prop="systemCode">
                     <el-input v-model="form.systemCode" placeholder="如: claim-system" />
                  </el-form-item>
               </el-col>
            </el-row>
            <el-form-item label="所属部门" prop="deptId">
               <el-tree-select
                  v-model="form.deptId"
                  :data="deptOptions"
                  :props="{ value: 'id', label: 'label', children: 'children' }"
                  value-key="id"
                  placeholder="请选择所属部门"
                  clearable
                  check-strictly
               />
            </el-form-item>
            <el-form-item label="管理用户" prop="managerIds">
               <el-select
                  v-model="managerIdList"
                  multiple
                  filterable
                  placeholder="请选择管理用户"
                  style="width: 100%"
                  @change="handleManagerChange"
               >
                  <el-option
                     v-for="user in userList"
                     :key="user.userId"
                     :label="user.nickName"
                     :value="user.userId"
                  />
               </el-select>
            </el-form-item>
            <el-form-item label="状态" prop="status">
               <el-radio-group v-model="form.status">
                  <el-radio value="0">正常</el-radio>
                  <el-radio value="1">停用</el-radio>
               </el-radio-group>
            </el-form-item>
            <el-form-item label="备注" prop="remark">
               <el-input v-model="form.remark" type="textarea" placeholder="请输入备注" />
            </el-form-item>
         </el-form>
         <template #footer>
            <div class="dialog-footer">
               <el-button type="primary" @click="submitForm">确 定</el-button>
               <el-button @click="cancel">取 消</el-button>
            </div>
         </template>
      </el-dialog>
   </div>
</template>

<script setup name="BusinessSystemManage">
import { listBusinessSystem, getBusinessSystem, delBusinessSystem, addBusinessSystem, updateBusinessSystem } from "@/api/system/businessSystem"
import { listUser, deptTreeSelect } from "@/api/system/user"

const { proxy } = getCurrentInstance()
const { sys_normal_disable } = useDict("sys_normal_disable")

const systemList = ref([])
const open = ref(false)
const loading = ref(true)
const showSearch = ref(true)
const total = ref(0)
const title = ref("")
const deptOptions = ref([])
const userList = ref([])
const managerIdList = ref([])

const data = reactive({
   form: {},
   queryParams: {
      pageNum: 1,
      pageSize: 10,
      systemName: undefined,
      systemCode: undefined,
      status: undefined,
      deptId: undefined
   },
   rules: {
      systemName: [{ required: true, message: "业务系统名称不能为空", trigger: "blur" }],
      systemCode: [{ required: true, message: "业务系统编码不能为空", trigger: "blur" }]
   }
})

const { queryParams, form, rules } = toRefs(data)

/** 查询列表 */
function getList() {
   loading.value = true
   listBusinessSystem(queryParams.value).then(response => {
      systemList.value = response.rows
      total.value = response.total
      loading.value = false
   })
}

/** 加载用户列表 */
function loadUserList() {
   listUser({ pageNum: 1, pageSize: 1000, status: "0" }).then(response => {
      userList.value = response.rows
   })
}

/** 加载部门树 */
function loadDeptTree() {
   if (deptOptions.value && deptOptions.value.length > 0) return
   deptTreeSelect().then(response => {
      deptOptions.value = response.data
   })
}

/** 取消按钮 */
function cancel() {
   open.value = false
   reset()
}

/** 表单重置 */
function reset() {
   form.value = {
      systemId: undefined,
      systemName: undefined,
      systemCode: undefined,
      deptId: undefined,
      managerIds: undefined,
      status: "0",
      remark: undefined
   }
   managerIdList.value = []
   proxy.resetForm("systemRef")
}

/** 管理用户选择变化 */
function handleManagerChange(val) {
   form.value.managerIds = val.join(",")
}

/** 搜索按钮操作 */
function handleQuery() {
   queryParams.value.pageNum = 1
   getList()
}

/** 重置按钮操作 */
function resetQuery() {
   proxy.resetForm("queryRef")
   handleQuery()
}

/** 新增按钮操作 */
function handleAdd() {
   reset()
   open.value = true
   title.value = "新增业务系统"
   if (userList.value.length === 0) {
      loadUserList()
   }
   loadDeptTree()
}

/** 修改按钮操作 */
function handleUpdate(row) {
   reset()
   getBusinessSystem(row.systemId).then(response => {
      form.value = response.data
      if (form.value.managerIds) {
         managerIdList.value = form.value.managerIds.split(",").map(id => parseInt(id)).filter(id => !isNaN(id))
      }
      open.value = true
      title.value = "修改业务系统"
      if (userList.value.length === 0) {
         loadUserList()
      }
   })
}

/** 提交按钮 */
function submitForm() {
   proxy.$refs["systemRef"].validate(valid => {
      if (valid) {
         if (form.value.systemId != undefined) {
            updateBusinessSystem(form.value).then(() => {
               proxy.$modal.msgSuccess("修改成功")
               open.value = false
               getList()
            })
         } else {
            addBusinessSystem(form.value).then(() => {
               proxy.$modal.msgSuccess("新增成功")
               open.value = false
               getList()
            })
         }
      }
   })
}

/** 删除按钮操作 */
function handleDelete(row) {
   proxy.$modal.confirm('是否确认删除业务系统"' + row.systemName + '"？').then(() => {
      return delBusinessSystem(row.systemId)
   }).then(() => {
      getList()
      proxy.$modal.msgSuccess("删除成功")
   }).catch(() => {})
}

getList()
</script>
