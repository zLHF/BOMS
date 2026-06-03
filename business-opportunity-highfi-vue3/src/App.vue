<template>
  <div class="app-root">
    <section v-if="currentPage === 'login'" class="login-page">
      <div class="login-card glass">
        <div class="brand-xl"><span class="logo-mark">M</span><div><h1>商机管理系统</h1><p>多租户 · 多用户 · 全流程商机协同</p></div></div>
        <el-form label-position="top" class="login-form">
          <el-form-item label="租户编码"><el-input v-model="login.tenant" size="large" prefix-icon="OfficeBuilding" /></el-form-item>
          <el-form-item label="账号"><el-input v-model="login.user" size="large" prefix-icon="User" /></el-form-item>
          <el-form-item label="密码"><el-input v-model="login.pass" type="password" show-password size="large" prefix-icon="Lock" /></el-form-item>
          <div class="login-tools"><el-checkbox v-model="login.remember">记住登录状态</el-checkbox><a>忘记密码？</a></div>
          <el-button class="full" type="primary" size="large" @click="currentPage='dashboard'">登录系统</el-button>
        </el-form>
      </div>
      <div class="login-hero">
        <div class="hero-card">
          <p class="eyebrow">Enterprise CRM Prototype</p>
          <h2>从线索、商机、报价到成交的一体化增长中台</h2>
          <p>支持租户隔离、组织权限、协作跟进、商机阶段流转、客户全景档案与经营分析。</p>
          <div class="hero-grid"><div><b>128</b><span>活跃商机</span></div><div><b>¥368万</b><span>成交金额</span></div><div><b>60%</b><span>重点项目赢率</span></div></div>
        </div>
      </div>
    </section>

    <template v-else>
      <aside class="sidebar">
        <div class="brand" @click="currentPage='dashboard'"><span class="logo-mark">M</span><b>商机管理系统</b></div>
        <div class="nav-scroll">
          <div v-for="group in menu" :key="group.title" class="nav-group">
            <div class="nav-title">{{ group.title }}</div>
            <button v-for="item in group.children" :key="item.key" :class="['nav-item', { active: currentPage === item.key }]" @click="currentPage=item.key">
              <el-icon><component :is="item.icon" /></el-icon><span>{{ item.label }}</span><em v-if="item.badge">{{ item.badge }}</em>
            </button>
          </div>
        </div>
        <div class="sidebar-foot"><span>仅本租户可见</span><a>按权限查看</a><small>版本 v3.0.0 High-Fi</small></div>
      </aside>

      <main class="main-shell">
        <header class="topbar">
          <div class="tenant-switch"><span>租户：华东销售中心</span><el-tag size="small" type="success">当前租户</el-tag><el-icon><ArrowDown /></el-icon></div>
          <el-input class="global-search" size="large" placeholder="搜索商机、客户、联系人、订单、附件等..." prefix-icon="Search"><template #append>⌘K</template></el-input>
          <div class="top-actions"><el-badge :value="12"><el-button circle :icon="Bell" /></el-badge><el-button circle :icon="QuestionFilled" /><el-button circle :icon="Setting" /><div class="user-chip"><img src="https://api.dicebear.com/7.x/avataaars/svg?seed=zhangwei" /><div><b>张伟</b><span>销售经理</span></div><el-icon><ArrowDown /></el-icon></div></div>
        </header>

        <section class="page-body">
          <Dashboard v-if="currentPage==='dashboard'" @open="currentPage='opportunities'" />
          <OpportunityList v-else-if="currentPage==='opportunities'" @detail="detailOpen=true" />
          <OpportunityForm v-else-if="currentPage==='opportunity-add'" />
          <ImportPage v-else-if="currentPage==='opportunity-import'" />
          <OpportunityDetail v-else-if="currentPage==='opportunity-detail'" />
          <CustomerDetail v-else-if="currentPage==='customer-detail'" />
          <GenericTablePage v-else :page="pageMap[currentPage]" />
        </section>

        <OpportunityDrawer v-model="detailOpen" />
      </main>
    </template>
  </div>
</template>

<script setup>
import { ref, computed, defineComponent } from 'vue'
import { Bell, QuestionFilled, Setting } from '@element-plus/icons-vue'

const currentPage = ref('opportunities')
const detailOpen = ref(false)
const login = ref({ tenant: 'hd-sales', user: 'zhangwei', pass: '123456', remember: true })

const menu = [
  { title: '核心业务', children: [
    { key:'dashboard', label:'工作台', icon:'DataBoard' },
    { key:'opportunities', label:'全部商机', icon:'TrendCharts', badge:'128' },
    { key:'opportunity-add', label:'新增商机', icon:'CirclePlus' },
    { key:'opportunity-import', label:'批量导入', icon:'UploadFilled' },
    { key:'opportunity-detail', label:'商机详情', icon:'Tickets' },
    { key:'customer-detail', label:'客户详情', icon:'OfficeBuilding' },
  ]},
  { title: '销售协同', children: [
    { key:'my-opportunities', label:'我的商机', icon:'User' },
    { key:'sub-opportunities', label:'下属商机', icon:'Connection' },
    { key:'coop-opportunities', label:'我协作的', icon:'Share' },
    { key:'sub-coop', label:'下属协作', icon:'SetUp' },
    { key:'follow-records', label:'跟进记录', icon:'ChatDotRound' },
    { key:'sms-center', label:'短信中心', icon:'Message' },
  ]},
  { title: '交易履约', children: [
    { key:'quotes', label:'报价管理', icon:'Money' },
    { key:'orders', label:'订单管理', icon:'ShoppingCart' },
    { key:'expenses', label:'费用管理', icon:'Wallet' },
    { key:'won-opportunities', label:'成交商机', icon:'Trophy' },
  ]},
  { title: '平台管理', children: [
    { key:'tenant-management', label:'租户管理', icon:'OfficeBuilding' },
    { key:'org-users', label:'组织用户', icon:'Avatar' },
    { key:'roles', label:'角色权限', icon:'Lock' },
    { key:'audit-logs', label:'操作日志', icon:'DocumentChecked' },
  ]},
]

const pageMap = {
  'my-opportunities': { title:'我的商机', desc:'展示当前登录人负责的商机、待跟进事项与风险提醒。', tabs:['全部','今日待跟进','逾期','高意向'], type:'opportunity' },
  'sub-opportunities': { title:'下属商机', desc:'按组织架构查看下属销售人员商机进展，支持主管分配和转移。', tabs:['全部下属','本月新增','即将成交','逾期未跟进'], type:'opportunity' },
  'coop-opportunities': { title:'我协作的', desc:'展示当前用户以协作人身份参与的商机，可写跟进、传附件、创建任务。', tabs:['协作中','待我处理','已完成'], type:'opportunity' },
  'sub-coop': { title:'下属协作', desc:'跟踪团队成员参与的跨部门协作任务。', tabs:['全部','售前支持','交付支持','法务支持'], type:'task' },
  'follow-records': { title:'跟进记录', desc:'统一沉淀商机与客户沟通记录，形成可审计的销售过程。', tabs:['全部','电话','拜访','会议','邮件'], type:'follow' },
  'sms-center': { title:'短信中心', desc:'支持短信模板、批量收件人、定时发送、发送记录与合规审核。', tabs:['发送短信','模板库','发送记录','审核配置'], type:'sms' },
  'quotes': { title:'报价管理', desc:'管理商机产品报价、报价单版本、折扣审批与报价附件。', tabs:['全部报价','待审批','已通过','已作废'], type:'quote' },
  'orders': { title:'订单管理', desc:'商机成交后的订单记录、回款状态、合同与发票关联。', tabs:['全部订单','待回款','执行中','已完成'], type:'order' },
  'expenses': { title:'费用管理', desc:'记录商机跟进过程中产生的差旅、招待、样品、售前等费用。', tabs:['全部费用','待审批','已报销','超预算'], type:'expense' },
  'won-opportunities': { title:'成交商机', desc:'沉淀已赢单商机，用于复盘来源、周期、报价与转化效率。', tabs:['本月成交','季度成交','大客户','续约'], type:'opportunity' },
  'tenant-management': { title:'租户管理', desc:'平台级管理多个业务租户，支持租户套餐、状态、数据隔离策略。', tabs:['全部租户','启用中','试用','停用'], type:'tenant' },
  'org-users': { title:'组织用户', desc:'配置部门、岗位、上下级关系和用户账号，支撑数据权限计算。', tabs:['组织架构','用户账号','岗位职级','邀请记录'], type:'user' },
  'roles': { title:'角色权限', desc:'配置角色菜单权限、按钮权限、字段权限与数据范围。', tabs:['角色列表','菜单权限','数据权限','字段权限'], type:'role' },
  'audit-logs': { title:'操作日志', desc:'记录用户对商机、客户、报价、订单、附件等对象的关键操作。', tabs:['全部日志','登录日志','数据变更','导入导出'], type:'log' },
}

const opportunities = [
  ['某能源集团数字化平台项目','某能源集团有限公司','方案报价','¥1,280,000','张伟','官网咨询','2024-05-20 14:30'],
  ['智慧园区解决方案','绿城物业服务集团','需求确认','¥680,000','李娜','市场活动','2024-05-20 11:20'],
  ['制造业MES系统升级','华东精密制造有限公司','商务谈判','¥950,000','王强','客户转介绍','2024-05-19 16:45'],
  ['零售连锁会员系统建设','悦购连锁超市','初步接触','¥320,000','陈晨','电话咨询','2024-05-19 10:15'],
  ['金融风控系统集成','浙商银行股份有限公司','输单','¥0','赵磊','投标','2024-05-18 09:30'],
  ['集团OA一体化项目','天合控股集团','赢单','¥450,000','李娜','老客户续约','2024-05-17 18:05'],
  ['数据中台建设项目','宁波港集团','方案报价','¥1,800,000','张伟','官网咨询','2024-05-17 14:22'],
]
const stageClass = s => ({'初步接触':'gray','需求确认':'cyan','方案报价':'blue','商务谈判':'orange','赢单':'green','输单':'red'}[s] || 'blue')

const PageHeader = defineComponent({ props:['title','desc'], template:`<div class="page-head"><div><div class="breadcrumb">商机管理 / {{ title }}</div><h1>{{ title }} <el-tag effect="light">仅本租户可见</el-tag></h1><p>{{ desc }}</p></div><slot /></div>` })

const KpiCards = defineComponent({ template:`<div class="kpi-grid"><div v-for="k in kpis" :key="k.t" class="kpi-card"><div :class="['kpi-icon',k.c]"><el-icon><component :is="k.i" /></el-icon></div><div><span>{{k.t}}</span><b>{{k.v}}</b><small>{{k.d}}</small></div></div></div>`, setup(){ return { kpis:[{t:'商机总数',v:'128',d:'较上月 ↑12.5%',i:'Briefcase',c:'blue'},{t:'本月新增',v:'28',d:'较上月 ↑27.3%',i:'CirclePlus',c:'green'},{t:'跟进中',v:'86',d:'占比 67.2%',i:'Refresh',c:'purple'},{t:'高意向',v:'32',d:'占比 25.0%',i:'HotWater',c:'orange'},{t:'已成交',v:'18',d:'较上月 ↑5',i:'SuccessFilled',c:'green'},{t:'成交金额',v:'¥3,680,000',d:'较上月 ↑18.6%',i:'Money',c:'blue'}] } } })

const OpportunityTable = defineComponent({ emits:['detail'], template:`<div class="table-card"><div class="table-toolbar"><span>已选择 0 项</span><div><el-button :icon="Grid">自定义列</el-button><el-button :icon="Filter">筛选</el-button><el-button>按更新时间排序 <el-icon><ArrowDown /></el-icon></el-button></div></div><table class="data-table"><thead><tr><th><input type="checkbox"></th><th>商机名称</th><th>客户</th><th>当前阶段</th><th>预计金额</th><th>负责人</th><th>协作人</th><th>下次跟进</th><th>来源</th><th>更新时间</th><th>操作</th></tr></thead><tbody><tr v-for="(r,i) in rows" :key="r[0]" :class="{selected:i===0}"><td><input type="checkbox" :checked="i===0"></td><td><a class="link" @click="$emit('detail')">{{r[0]}}</a></td><td>{{r[1]}}</td><td><span :class="['stage', cls(r[2])]">{{r[2]}}</span></td><td><b>{{r[3]}}</b></td><td><span class="person"><img :src="avatar(r[4])">{{r[4]}}</span></td><td><span class="avatar-stack"><img v-for="n in 3" :src="avatar(r[0]+n)"><em v-if="i%2===0">+{{i+1}}</em></span></td><td><b>{{ i===4 || i===5 ? '--' : '2024-05-'+(22+i) }}</b><small :class="i<2?'green-text':'blue-text'">{{i===0?'今天': i<5?(i+1)+'天后':''}}</small></td><td>{{r[5]}}</td><td>{{r[6]}}</td><td><a @click="$emit('detail')">查看</a><a>更多</a></td></tr></tbody></table><div class="pager"><span>共 128 条</span><el-pagination background layout="prev, pager, next" :total="128" :page-size="20" /></div></div>`, setup(){ return { rows: opportunities, cls: stageClass, avatar:(s)=>`https://api.dicebear.com/7.x/avataaars/svg?seed=${encodeURIComponent(s)}` } } })

const OpportunityList = defineComponent({ components:{PageHeader,KpiCards,OpportunityTable}, emits:['detail'], template:`<div><PageHeader title="全部商机" desc="统一管理租户内所有商机，支持分阶段跟进、协作人、报价、费用、订单与操作日志。"><div class="head-actions"><el-button type="primary" :icon="Plus">新增商机</el-button><el-button>批量导入</el-button><el-button>批量分配</el-button><el-button>导出 <el-icon><ArrowDown /></el-icon></el-button></div></PageHeader><KpiCards/><div class="filter-card"><el-row :gutter="16"><el-col :span="5"><label>商机名称</label><el-input placeholder="请输入商机名称" /></el-col><el-col :span="5"><label>客户名称</label><el-input placeholder="请输入客户名称" /></el-col><el-col :span="4"><label>当前阶段</label><el-select placeholder="请选择阶段"><el-option label="方案报价" value="a" /></el-select></el-col><el-col :span="4"><label>负责人</label><el-select placeholder="请选择负责人"><el-option label="张伟" value="z" /></el-select></el-col><el-col :span="4"><label>来源渠道</label><el-select placeholder="请选择来源"><el-option label="官网咨询" value="w" /></el-select></el-col><el-col :span="2" class="filter-btn"><el-button type="primary">查询</el-button></el-col></el-row><el-row :gutter="16" class="mt16"><el-col :span="6"><label>时间范围</label><el-date-picker type="daterange" start-placeholder="开始日期" end-placeholder="结束日期" /></el-col><el-col :span="4"><label>标签</label><el-select placeholder="请选择标签"><el-option label="重点客户" value="k" /></el-select></el-col><el-col :span="6" class="filter-btn left"><el-button>重置</el-button><el-button link type="primary">展开 <el-icon><ArrowDown /></el-icon></el-button></el-col></el-row></div><OpportunityTable @detail="$emit('detail')"/></div>` })

const Dashboard = defineComponent({ components:{PageHeader,KpiCards}, emits:['open'], template:`<div><PageHeader title="工作台" desc="销售经理的一日经营视图：待办、商机漏斗、团队业绩和风险提醒。"><el-button type="primary" @click="$emit('open')">查看全部商机</el-button></PageHeader><KpiCards/><div class="dashboard-grid"><div class="panel big"><h3>销售漏斗</h3><div class="funnel"><div style="--w:92%"><b>初步接触</b><span>32</span></div><div style="--w:78%"><b>需求确认</b><span>28</span></div><div style="--w:66%"><b>方案报价</b><span>24</span></div><div style="--w:48%"><b>商务谈判</b><span>16</span></div><div style="--w:34%"><b>赢单</b><span>18</span></div></div></div><div class="panel"><h3>今日待办</h3><div v-for="i in 5" class="todo"><el-checkbox /> <div><b>{{['联系刘明确认报价','审批王强折扣申请','上传售前方案附件','复盘输单原因','安排客户拜访'][i-1]}}</b><p>截止 {{i+9}}:30 · {{i%2?'高优先级':'普通'}}</p></div></div></div><div class="panel"><h3>团队排行榜</h3><div class="rank" v-for="(n,i) in ['张伟','李娜','王强','陈晨']"><span>{{i+1}}</span><img :src="avatar(n)"><b>{{n}}</b><em>¥{{[128,98,76,52][i]}}万</em></div></div></div></div>`, setup(){return { avatar:(s)=>`https://api.dicebear.com/7.x/avataaars/svg?seed=${s}` }} })

const OpportunityDrawer = defineComponent({ props:['modelValue'], emits:['update:modelValue'], template:`<el-drawer :model-value="modelValue" @update:modelValue="$emit('update:modelValue',$event)" size="420px" class="pretty-drawer" :with-header="false"><div class="drawer-head"><h2>商机详情</h2><el-button circle text :icon="Close" @click="$emit('update:modelValue',false)" /></div><div class="detail-title"><el-tag>方案报价</el-tag><span>ID：OPP-20240520-001</span><h3>某能源集团数字化平台项目 <el-icon><Star /></el-icon></h3><el-tag type="info">仅本租户可见</el-tag></div><div class="steps"><div class="done">初步接触</div><div class="done">需求确认</div><div class="active">方案报价</div><div>商务谈判</div><div>赢单</div></div><div class="info-grid"><div><span>客户</span><b>某能源集团有限公司</b></div><div><span>关键联系人</span><b>刘明 CTO</b><small>138 8888 8888</small></div><div><span>预计金额</span><b>¥1,280,000</b></div><div><span>预计成交日期</span><b>2024-06-15</b></div></div><div class="owner-line"><img :src="avatar('张伟')"><div><span>负责人</span><b>张伟 · 销售经理</b></div><div class="avatar-stack"><img v-for="n in 5" :src="avatar('协作'+n)"><em>+2</em></div></div><div class="timeline"><h4>最新跟进记录 <a>查看全部</a></h4><div class="time-item"><i></i><b>2024-05-20 14:30 张伟</b><p>与刘总沟通方案细节，对方对数据可视化模块比较关注，已安排技术方案演示。</p><el-tag size="small">电话沟通</el-tag><el-tag size="small" type="info">附件(2)</el-tag></div></div><div class="drawer-actions"><el-button type="primary">写跟进</el-button><el-button>新建任务</el-button><el-button>添加协作</el-button></div><div class="mini-chart"><h4>阶段分布（全部商机）</h4><p><span style="--w:82%"></span><b>初步接触 32</b></p><p><span style="--w:70%"></span><b>需求确认 28</b></p><p><span style="--w:60%"></span><b>方案报价 24</b></p></div></el-drawer>`, setup(){return {avatar:(s)=>`https://api.dicebear.com/7.x/avataaars/svg?seed=${s}`}} })

const OpportunityDetail = defineComponent({ template:`<div><PageHeader title="商机详情" desc="完整展示商机阶段、基础信息、联系人、跟进、任务、订单、报价、费用、附件与操作日志。"><el-button type="primary">写跟进</el-button><el-button>转移商机</el-button></PageHeader><div class="detail-layout"><div class="detail-main"><div class="stage-bar"><span class="done">初步接触</span><span class="done">需求确认</span><span class="active">方案报价</span><span>商务谈判</span><span>赢单</span></div><el-tabs model-value="overview"><el-tab-pane label="概况信息" name="overview"><RecordGrid /></el-tab-pane><el-tab-pane label="联系人" name="contacts"><ContactCards /></el-tab-pane><el-tab-pane label="跟进记录" name="follow"><ActivityList /></el-tab-pane><el-tab-pane label="任务记录" name="task"><ActivityList /></el-tab-pane><el-tab-pane label="关联订单" name="order"><MiniTable type="order" /></el-tab-pane><el-tab-pane label="产品报价" name="quote"><MiniTable type="quote" /></el-tab-pane><el-tab-pane label="费用记录" name="fee"><MiniTable type="fee" /></el-tab-pane><el-tab-pane label="相关附件" name="file"><MiniTable type="file" /></el-tab-pane><el-tab-pane label="操作日志" name="log"><MiniTable type="log" /></el-tab-pane></el-tabs></div><div class="side-panel"><h3>关键指标</h3><div class="metric"><span>预计金额</span><b>¥1,280,000</b></div><div class="metric"><span>成交概率</span><b>60%</b></div><div class="metric"><span>下次跟进</span><b>今天 17:00</b></div><el-button type="primary" class="full">添加协作人</el-button></div></div></div>` })

const CustomerDetail = defineComponent({ template:`<div><PageHeader title="客户详情" desc="客户全景档案：资料、联系人、下属客户、跟进、任务、工单、商机、订单、发票、退款、费用、附件、归属和日志。"><el-button type="primary">编辑客户</el-button><el-button>转移客户</el-button></PageHeader><div class="customer-hero"><div><h2>某能源集团有限公司</h2><p>大型能源集团 · 华东区域 · A级客户 · 仅本租户可见</p><div><el-tag>重点客户</el-tag><el-tag type="success">活跃</el-tag><el-tag type="warning">高价值</el-tag></div></div><div class="customer-stats"><b>7</b><span>关联商机</span><b>¥368万</b><span>累计订单</span><b>12</b><span>联系人</span></div></div><el-tabs model-value="profile" class="pretty-tabs"><el-tab-pane label="客户概况" name="profile"><RecordGrid /></el-tab-pane><el-tab-pane label="客户资料详情" name="data"><RecordGrid /></el-tab-pane><el-tab-pane label="联系人" name="contact"><ContactCards /></el-tab-pane><el-tab-pane label="下级客户" name="children"><MiniTable /></el-tab-pane><el-tab-pane label="跟进记录" name="follow"><ActivityList /></el-tab-pane><el-tab-pane label="联系记录" name="call"><ActivityList /></el-tab-pane><el-tab-pane label="任务记录" name="task"><ActivityList /></el-tab-pane><el-tab-pane label="工单记录" name="ticket"><MiniTable /></el-tab-pane><el-tab-pane label="商机记录" name="opp"><OpportunityTable /></el-tab-pane><el-tab-pane label="订单/发票/退款/费用" name="money"><MiniTable /></el-tab-pane><el-tab-pane label="附件/归属/日志" name="other"><MiniTable /></el-tab-pane></el-tabs></div>` })

const OpportunityForm = defineComponent({ template:`<div><PageHeader title="新增商机" desc="录入单个商机，自动关联客户、联系人、负责人、协作人和租户数据范围。"><el-button type="primary">保存商机</el-button><el-button>保存并新增</el-button></PageHeader><div class="form-card"><el-form label-position="top"><h3>基础信息</h3><el-row :gutter="20"><el-col :span="8"><el-form-item label="商机名称"><el-input placeholder="请输入商机名称" /></el-form-item></el-col><el-col :span="8"><el-form-item label="关联客户"><el-select placeholder="选择客户"><el-option label="某能源集团有限公司" value="1" /></el-select></el-form-item></el-col><el-col :span="8"><el-form-item label="当前阶段"><el-select placeholder="选择阶段"><el-option label="初步接触" value="1" /></el-select></el-form-item></el-col><el-col :span="8"><el-form-item label="预计金额"><el-input placeholder="¥" /></el-form-item></el-col><el-col :span="8"><el-form-item label="负责人"><el-select placeholder="选择负责人"><el-option label="张伟" value="1" /></el-select></el-form-item></el-col><el-col :span="8"><el-form-item label="协作人"><el-select multiple placeholder="选择协作人"><el-option label="售前-李娜" value="1" /></el-select></el-form-item></el-col><el-col :span="24"><el-form-item label="客户需求"><el-input type="textarea" :rows="4" placeholder="描述客户痛点、预算、决策链、竞品情况等" /></el-form-item></el-col></el-row><h3>权限与归属</h3><el-row :gutter="20"><el-col :span="8"><el-form-item label="所属租户"><el-input disabled value="华东销售中心" /></el-form-item></el-col><el-col :span="8"><el-form-item label="数据可见范围"><el-select placeholder="选择"><el-option label="负责人及协作人可见" value="1" /></el-select></el-form-item></el-col><el-col :span="8"><el-form-item label="商机池策略"><el-select placeholder="选择"><el-option label="30天未跟进自动提醒" value="1" /></el-select></el-form-item></el-col></el-row></el-form></div></div>` })

const ImportPage = defineComponent({ template:`<div><PageHeader title="批量导入商机" desc="下载模板、上传 Excel、字段映射、预校验、确认导入，适合市场活动线索批量转商机。"><el-button type="primary">下载模板</el-button></PageHeader><div class="import-steps"><div class="active">1 上传文件</div><div>2 字段映射</div><div>3 数据校验</div><div>4 导入结果</div></div><div class="upload-card"><el-upload drag action="#"><el-icon class="el-icon--upload"><UploadFilled /></el-icon><div class="el-upload__text">拖拽 Excel 到此处，或 <em>点击上传</em></div><template #tip><div class="el-upload__tip">支持 .xlsx，单次最多 5000 条，导入数据默认归属当前租户</div></template></el-upload></div><MiniTable /></div>` })

const GenericTablePage = defineComponent({ props:['page'], template:`<div><PageHeader :title="page.title" :desc="page.desc"><el-button type="primary">新增</el-button><el-button>导出</el-button></PageHeader><div class="tabs-card"><el-tabs :model-value="page.tabs[0]"><el-tab-pane v-for="t in page.tabs" :key="t" :label="t" :name="t" /></el-tabs><div class="filter-card compact"><el-input placeholder="关键词搜索" prefix-icon="Search" /><el-select placeholder="状态"><el-option label="启用" value="1" /></el-select><el-date-picker type="daterange" start-placeholder="开始日期" end-placeholder="结束日期" /><el-button type="primary">查询</el-button><el-button>重置</el-button></div><MiniTable :type="page.type" /></div></div>` })

const RecordGrid = defineComponent({ template:`<div class="record-grid"><div v-for="item in items"><span>{{item[0]}}</span><b>{{item[1]}}</b></div></div>`, setup(){return {items:[['商机来源','官网咨询'],['商机阶段','方案报价'],['预计金额','¥1,280,000'],['负责人','张伟'],['成交概率','60%'],['预计成交日期','2024-06-15'],['客户行业','能源/制造'],['竞争情况','2家竞品参与'],['潜在风险','预算审批周期较长'],['数据范围','当前租户 + 协作人']]} } })
const ContactCards = defineComponent({ template:`<div class="contact-grid"><div class="contact-card" v-for="n in ['刘明 CTO','王莉 采购经理','陈磊 信息化主任']"><img :src="avatar(n)"><div><b>{{n}}</b><p>138 8888 8888 · liming@example.com</p><el-tag size="small">关键联系人</el-tag></div><el-button>联系</el-button></div></div>`, setup(){return {avatar:(s)=>`https://api.dicebear.com/7.x/avataaars/svg?seed=${s}`}} })
const ActivityList = defineComponent({ template:`<div class="activity-list"><div v-for="i in 5" class="activity"><i></i><div><b>{{['电话沟通客户需求','现场拜访客户总部','售前方案评审','发送报价单','安排产品演示'][i-1]}}</b><p>2024-05-{{20-i}} 14:30 · 张伟 · 已同步给协作人</p><span>记录客户反馈、下一步计划、附件与任务提醒，支持权限范围内查看。</span></div></div></div>` })
const MiniTable = defineComponent({ props:['type'], template:`<div class="mini-table"><table><thead><tr><th>名称</th><th>状态</th><th>负责人</th><th>金额/数量</th><th>更新时间</th><th>操作</th></tr></thead><tbody><tr v-for="i in 6"><td>{{name(i)}}</td><td><span class="stage blue">{{status(i)}}</span></td><td>张伟</td><td>{{money(i)}}</td><td>2024-05-{{20-i}} 10:2{{i}}</td><td><a>查看</a><a>编辑</a></td></tr></tbody></table></div>`, setup(props){return {name:i=>({sms:'短信模板 '+i,tenant:'租户 '+i,user:'用户账号 '+i,role:'角色权限 '+i,log:'操作日志 '+i,quote:'报价单 V'+i,order:'订单 '+i,expense:'费用记录 '+i,file:'附件 '+i}[props.type]||'业务记录 '+i),status:i=>['启用','待审批','已完成','处理中'][i%4],money:i=> props.type==='tenant'?`${20+i} 用户`:`¥${i*12},000`}} })
</script>
