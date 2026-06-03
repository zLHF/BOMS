<template>
  <div v-if="route==='login'" class="login-page">
    <div class="login-card">
      <div class="brand big"><span class="logo">M</span><div><b>商机管理系统</b><small>多用户 · 多租户 · 协同销售</small></div></div>
      <h1>欢迎登录</h1>
      <p>统一账号登录，按租户、组织、角色自动加载数据范围。</p>
      <label>租户</label><select v-model="tenant"><option v-for="t in tenants" :key="t">{{t}}</option></select>
      <label>账号</label><input value="zhangwei" />
      <label>密码</label><input value="123456" type="password" />
      <button class="primary full" @click="route='dashboard'">登录系统</button>
      <div class="login-meta">演示：销售经理 / 华东销售中心 / 本租户数据</div>
    </div>
    <div class="login-hero"><h2>从线索到成交，全流程掌控</h2><p>商机、客户、跟进、报价、订单、费用、附件与操作日志统一管理。</p></div>
  </div>

  <div v-else class="app-shell">
    <aside class="sidebar">
      <div class="brand"><span class="logo">M</span><b>商机管理系统</b></div>
      <nav>
        <div v-for="item in nav" :key="item.key" class="nav-group">
          <button :class="['nav-main', {active: currentGroup===item.key}]" @click="openNav(item)"><span>{{item.icon}}</span>{{item.name}}<em v-if="item.children">⌄</em></button>
          <div v-if="item.children && currentGroup===item.key" class="subnav">
            <button v-for="c in item.children" :key="c.key" :class="{active: route===c.key}" @click="route=c.key">{{c.name}}</button>
          </div>
        </div>
      </nav>
      <div class="sidebar-foot"><span>仅本租户可见</span><a>按权限查看</a><small>版本：v3.0.0</small></div>
    </aside>

    <main class="main">
      <header class="topbar">
        <div class="tenant-select">租户：<select v-model="tenant"><option v-for="t in tenants" :key="t">{{t}}</option></select><span>当前租户</span></div>
        <div class="global-search">⌕ <input placeholder="搜索商机、客户、联系人、订单、附件等..."/><kbd>⌘K</kbd></div>
        <div class="top-icons"><span class="badge">🔔<i>12</i></span><span>?</span><span>⚙</span><img src="https://api.dicebear.com/7.x/avataaars/svg?seed=zhang"/><b>张伟</b><small>销售经理</small></div>
      </header>

      <section class="content">
        <component :is="pageComponent" :title="pageTitle" :variant="route" @go="route=$event" />
      </section>
    </main>
  </div>
</template>

<script setup>
import { ref, computed, defineComponent } from 'vue'

const route = ref('dashboard')
const tenant = ref('华东销售中心')
const tenants = ['华东销售中心','华南事业部','北方大区','集团总部']

const nav = [
  {key:'dashboard', name:'工作台', icon:'🏠'},
  {key:'opportunity', name:'商机管理', icon:'🎯', children:[
    {key:'opp-all',name:'全部商机'}, {key:'opp-mine',name:'我负责的'}, {key:'opp-collab',name:'我协作的'}, {key:'opp-follow',name:'我关注的'}, {key:'opp-sub',name:'下属商机'}, {key:'opp-sub-collab',name:'下属协作'}, {key:'opp-pool',name:'商机池'}, {key:'opp-won',name:'成交商机'}, {key:'opp-add',name:'新增商机'}, {key:'opp-import',name:'批量导入'}, {key:'opp-sms',name:'发送短信'}]},
  {key:'customer', name:'客户管理', icon:'👥', children:[{key:'customer-list',name:'客户列表'}, {key:'customer-detail',name:'客户详情'}, {key:'contact-list',name:'联系人管理'}, {key:'customer-owner',name:'客户归属'}]},
  {key:'follow-records', name:'跟进记录', icon:'📝'},
  {key:'quotes', name:'报价管理', icon:'💬'},
  {key:'orders', name:'订单管理', icon:'🛒'},
  {key:'expenses', name:'费用管理', icon:'💳'},
  {key:'sms-center', name:'短信中心', icon:'✉️'},
  {key:'collab-center', name:'协作中心', icon:'🤝'},
  {key:'reports', name:'报表分析', icon:'📊'},
  {key:'settings', name:'系统设置', icon:'⚙️', children:[{key:'tenant-manage',name:'租户管理'}, {key:'org-manage',name:'组织架构'}, {key:'user-manage',name:'用户管理'}, {key:'role-manage',name:'角色权限'}, {key:'data-permission',name:'数据权限'}, {key:'audit-log',name:'操作日志'}, {key:'system-param',name:'参数配置'}]}
]
const currentGroup = computed(()=> nav.find(n=> n.key===route.value || n.children?.some(c=>c.key===route.value))?.key || route.value)
function openNav(item){ route.value = item.children ? item.children[0].key : item.key }
const pageTitle = computed(()=>{
  for(const n of nav){ if(n.key===route.value) return n.name; const c=n.children?.find(x=>x.key===route.value); if(c) return c.name }
  return '工作台'
})
const pageComponent = computed(()=>{
  if(route.value==='dashboard') return Dashboard
  if(route.value.startsWith('opp-')) return OpportunityPage
  if(route.value.startsWith('customer') || route.value==='contact-list') return CustomerPage
  if(['tenant-manage','org-manage','user-manage','role-manage','data-permission','audit-log','system-param'].includes(route.value)) return SettingPage
  return GeneralPage
})

const opportunities = [
  ['某能源集团数字化平台项目','某能源集团有限公司','方案报价','¥1,280,000','张伟','官网咨询','2024-05-20 14:30'],
  ['智慧园区解决方案','绿城物业服务集团','需求确认','¥680,000','李娜','市场活动','2024-05-20 11:20'],
  ['制造业MES系统升级','华东精密制造有限公司','商务谈判','¥950,000','王强','客户转介绍','2024-05-19 16:45'],
  ['零售连锁会员系统建设','悦购连锁超市','初步接触','¥320,000','陈晨','电话咨询','2024-05-19 10:15'],
  ['金融风控系统集成','浙商银行股份有限公司','输单','¥0','赵磊','投标','2024-05-18 09:30'],
  ['集团OA一体化项目','天合控股集团','赢单','¥450,000','李娜','老客户续约','2024-05-17 18:05'],
  ['数据中台建设项目','宁波港集团','方案报价','¥1,800,000','张伟','官网咨询','2024-05-17 14:22'],
  ['供应链协同平台','双汇发展集团','需求确认','¥780,000','王强','行业展会','2024-05-16 09:55']
]
const customers = [
  ['某能源集团有限公司','集团客户','能源','A级','张伟','¥3,800,000','8'],
  ['绿城物业服务集团','企业客户','物业','B级','李娜','¥1,120,000','4'],
  ['华东精密制造有限公司','企业客户','制造','A级','王强','¥2,450,000','6'],
  ['悦购连锁超市','连锁客户','零售','C级','陈晨','¥620,000','2'],
  ['浙商银行股份有限公司','金融客户','金融','A级','赵磊','¥5,300,000','10']
]

const PageHeader = defineComponent({props:['title','sub'],emits:['go'],template:`<div class="page-head"><div><div class="crumb">{{sub||'商机管理'}} / {{title}}</div><h1>{{title}} <span class="pill blue">仅本租户可见</span></h1></div><div class="actions"><button class="primary" @click="$emit('go','opp-add')">+ 新增商机</button><button @click="$emit('go','opp-import')">批量导入</button><button>批量分配</button><button>导出⌄</button></div></div>`})
const Kpis = defineComponent({template:`<div class="kpis"><div class="kpi"><i>🎯</i><span>商机总数</span><b>128</b><em>较上月 ↑12.5%</em></div><div class="kpi"><i>➕</i><span>本月新增</span><b>28</b><em>较上月 ↑27.3%</em></div><div class="kpi"><i>🔄</i><span>跟进中</span><b>86</b><em>占比 67.2%</em></div><div class="kpi"><i>🔥</i><span>高意向</span><b>32</b><em>占比 25.0%</em></div><div class="kpi"><i>✅</i><span>已成交</span><b>18</b><em>较上月 ↑5</em></div><div class="kpi"><i>¥</i><span>成交金额</span><b>¥368万</b><em>较上月 ↑18.6%</em></div></div>`})
const FilterBar = defineComponent({template:`<div class="filter-card"><div class="filter-grid"><label>商机名称<input placeholder="请输入商机名称"/></label><label>客户名称<input placeholder="请输入客户名称"/></label><label>当前阶段<select><option>请选择阶段</option></select></label><label>负责人<select><option>请选择负责人</option></select></label><label>来源渠道<select><option>请选择来源</option></select></label><label>时间范围<input placeholder="开始日期 ~ 结束日期"/></label><label>标签<select><option>请选择标签</option></select></label><div class="filter-actions"><button class="primary">查询</button><button>重置</button><a>展开⌄</a></div></div></div>`})

const Dashboard = defineComponent({components:{Kpis},template:`<div><div class="page-head"><div><div class="crumb">工作台 / 概览</div><h1>销售工作台</h1></div><div class="actions"><button class="primary">今日跟进</button><button>新建任务</button></div></div><Kpis/><div class="dash-grid"><section class="card"><h3>我的待办</h3><div class="todo" v-for="x in ['今天需跟进 12 个商机','3 个报价待审批','2 个客户待分配','1 个合同即将到期']">{{x}}<button>处理</button></div></section><section class="card"><h3>销售漏斗</h3><div class="funnel"><span style="width:88%">初步接触 32</span><span style="width:76%">需求确认 28</span><span style="width:62%">方案报价 24</span><span style="width:45%">商务谈判 17</span><span style="width:31%">赢单 12</span></div></section><section class="card wide"><h3>最近动态</h3><div class="timeline"><p v-for="x in ['张伟更新了「某能源集团数字化平台项目」阶段为方案报价','李娜新增客户联系人：王总 / 采购负责人','系统完成华东销售中心数据权限同步','王强上传了MES项目报价单附件']">{{x}}<small>刚刚</small></p></div></section></div></div>`})

const Avatar = defineComponent({props:['name'],template:`<span class="avatar"><img :src="'https://api.dicebear.com/7.x/avataaars/svg?seed='+name"/>{{name}}</span>`})
const DetailPanel = defineComponent({template:`<aside class="detail"><h3>商机详情 <button>×</button></h3><span class="pill blue">方案报价</span><small>ID：OPP-20240520-001</small><h2>某能源集团数字化平台项目 ☆</h2><span class="pill">仅本租户可见</span><div class="steps"><span>初步接触</span><span>需求确认</span><b>方案报价</b><span>商务谈判</span><span>赢单</span></div><div class="info-grid"><p><label>客户</label>某能源集团有限公司</p><p><label>关键联系人</label>刘明 CTO<br>138 8888 8888</p><p><label>预计金额</label>¥1,280,000</p><p><label>预计成交日</label>2024-06-15</p></div><h4>最新跟进记录 <a>查看全部</a></h4><div class="note">2024-05-20 14:30 张伟<br>与刘总沟通方案细节，对方对数据可视化模块比较关注，已安排技术方案演示。</div><div class="quick"><button class="primary">写跟进</button><button>新建任务</button><button>添加协作</button></div><h4>阶段分布</h4><div class="mini-bars"><span style="width:90%">初步接触 32</span><span style="width:78%">需求确认 28</span><span style="width:66%">方案报价 24</span></div></aside>`})

const FormPage = defineComponent({props:['title'],template:`<div class="form-layout"><section class="card"><h3>{{title}}</h3><div class="form-grid"><label>商机名称<input value="某集团数字化平台项目"/></label><label>关联客户<select><option>某能源集团有限公司</option></select></label><label>当前阶段<select><option>初步接触</option><option>需求确认</option></select></label><label>预计金额<input value="1280000"/></label><label>来源渠道<select><option>官网咨询</option><option>市场活动</option></select></label><label>负责人<select><option>张伟</option></select></label><label>下次跟进时间<input type="date"/></label><label>协作人<input value="李娜、王强"/></label><label class="span2">客户需求<textarea>建设集团级数字化经营平台，关注数据可视化、权限隔离、审批流。</textarea></label><label class="span2">风险与备注<textarea>预算审批周期较长，需要技术方案演示。</textarea></label></div><div class="actions bottom"><button class="primary">保存</button><button>保存并新建跟进</button><button>取消</button></div></section><section class="card"><h3>权限设置</h3><p>数据范围：仅本租户可见</p><p>协作权限：可查看、可跟进、可上传附件</p><p>审批策略：预计金额超过 100 万需销售总监审批</p></section></div>`})
const ImportPage = defineComponent({template:`<div class="card import"><h3>批量导入商机</h3><div class="steps-row"><b>1 下载模板</b><b>2 上传文件</b><b>3 字段映射</b><b>4 导入结果</b></div><div class="upload-box">拖拽 Excel 文件到此处，或点击上传<br><button class="primary">选择文件</button><button>下载模板</button></div><table><thead><tr><th>字段</th><th>Excel列</th><th>校验规则</th></tr></thead><tbody><tr><td>商机名称</td><td>A列</td><td>必填，不超过100字</td></tr><tr><td>客户名称</td><td>B列</td><td>不存在时自动创建或进入待确认</td></tr><tr><td>负责人</td><td>C列</td><td>必须属于当前租户</td></tr></tbody></table></div>`})
const SmsPage = defineComponent({template:`<div class="sms-layout"><section class="card"><h3>收件号码</h3><button class="primary full">+ 添加商机联系人</button><button class="full">+ 添加客户联系人</button><div class="recipient">刘明 138****8888<br>王总 139****6666</div></section><section class="card"><h3>短信内容</h3><textarea class="sms-text">尊敬的客户您好，关于贵司数字化平台项目方案已更新，请查收。</textarea><div class="sms-meta">预计 1 条短信 / 当前租户剩余额度 1000 条</div><button class="primary">立即发送</button><button>保存草稿</button><button>定时发送</button></section></div>`})

const OpportunityPage = defineComponent({props:['title','variant'],emits:['go'],components:{PageHeader,Kpis,FilterBar,FormPage,ImportPage,SmsPage,DetailPanel,Avatar},setup(){return {opportunities}},template:`
<div>
  <PageHeader :title="title" @go="$emit('go',$event)" />
  <template v-if="variant==='opp-add'"><FormPage title="新增商机" /></template>
  <template v-else-if="variant==='opp-import'"><ImportPage /></template>
  <template v-else-if="variant==='opp-sms'"><SmsPage /></template>
  <template v-else>
    <Kpis/><FilterBar/>
    <div class="workspace">
      <div class="table-card"><div class="table-toolbar"><span>已选择 0 项</span><div><button>自定义列</button><button>筛选</button><button>按更新时间排序⌄</button></div></div><table><thead><tr><th><input type="checkbox"/></th><th>商机名称</th><th>客户</th><th>当前阶段</th><th>预计金额</th><th>负责人</th><th>协作人</th><th>下次跟进</th><th>来源</th><th>更新时间</th><th>操作</th></tr></thead><tbody><tr v-for="(r,i) in opportunities" :class="{selected:i===0}"><td><input type="checkbox" :checked="i===0"/></td><td><a @click="$emit('go','opp-detail')">{{r[0]}}</a></td><td>{{r[1]}}</td><td><span :class="['stage',r[2]]">{{r[2]}}</span></td><td>{{r[3]}}</td><td><Avatar :name="r[4]"/></td><td><div class="avatars"><span v-for="n in 3">👤</span><em v-if="i%2===0">+{{i+1}}</em></div></td><td>2024-05-2{{i}}<br><b class="green">{{i===0?'今天':(i+1)+'天后'}}</b></td><td>{{r[5]}}</td><td>{{r[6]}}</td><td><a @click="$emit('go','opp-detail')">查看</a> <a>更多⌄</a></td></tr></tbody></table><div class="pager">共128条 <button>1</button><button>2</button><button>3</button><button>下一页</button></div></div>
      <DetailPanel />
    </div>
  </template>
</div>`})


const CustomerPage = defineComponent({props:['title','variant'],emits:['go'],setup(){return {customers}},template:`<div><PageHeader :title="title" sub="客户管理" @go="$emit('go',$event)"/><template v-if="variant==='customer-detail'"><CustomerDetail /></template><template v-else><FilterBar/><div class="table-card"><table><thead><tr><th>客户名称</th><th>客户类型</th><th>行业</th><th>等级</th><th>负责人</th><th>成交金额</th><th>商机数</th><th>操作</th></tr></thead><tbody><tr v-for="r in customers"><td><a @click="$emit('go','customer-detail')">{{r[0]}}</a></td><td>{{r[1]}}</td><td>{{r[2]}}</td><td><span class="pill">{{r[3]}}</span></td><td><Avatar :name="r[4]"/></td><td>{{r[5]}}</td><td>{{r[6]}}</td><td><a @click="$emit('go','customer-detail')">详情</a> <a>转移</a></td></tr></tbody></table></div></template></div>`,components:{PageHeader,FilterBar,Avatar}})
const CustomerDetail = defineComponent({template:`<div class="detail-page"><section class="card"><h2>某能源集团有限公司</h2><p><span class="pill blue">集团客户</span> <span class="pill">A级客户</span> <span class="pill">仅本租户可见</span></p><div class="tabs"><button v-for="t in ['概况','资料详情','联系人','下级客户','跟进记录','联系记录','任务记录','工单记录','商机记录','订单记录','发票记录','退款记录','费用记录','附件','归属记录','操作日志']">{{t}}</button></div><div class="info-grid large"><p><label>客户行业</label>能源 / 国企</p><p><label>客户规模</label>5000人以上</p><p><label>负责人</label>张伟</p><p><label>所属租户</label>华东销售中心</p><p><label>财务信息</label>信用等级 A，账期 30 天</p><p><label>最新沟通</label>关注数据安全与可视化驾驶舱</p></div></section><section class="card"><h3>关联数据</h3><div class="relation-grid"><b>商机 8</b><b>订单 3</b><b>发票 12</b><b>工单 2</b><b>附件 16</b><b>费用 5</b></div></section></div>`})

const SettingPage = defineComponent({props:['title','variant'],template:`<div><PageHeader :title="title" sub="系统设置"/><div class="settings-grid"><section class="card"><h3>{{title}}</h3><table><thead><tr><th>名称</th><th>编码</th><th>状态</th><th>数据范围</th><th>操作</th></tr></thead><tbody><tr v-for="x in ['华东销售中心','华南事业部','北方大区','集团总部']"><td>{{x}}</td><td>TENANT-{{x.length}}0{{x.length}}</td><td><span class="pill green">启用</span></td><td>本租户 / 下级组织</td><td><a>编辑</a> <a>授权</a></td></tr></tbody></table></section><section class="card"><h3>权限矩阵</h3><div class="matrix"><p v-for="x in ['超级管理员：全租户管理','租户管理员：本租户配置','销售经理：团队数据','销售人员：本人及协作数据','财务：订单/发票/费用']">{{x}}</p></div></section></div></div>`,components:{PageHeader}})

const GeneralPage = defineComponent({props:['title','variant'],template:`<div><PageHeader :title="title"/><div class="generic-grid"><section class="card"><h3>{{title}}列表</h3><FilterBar/><table><thead><tr><th>编号</th><th>关联客户</th><th>关联商机</th><th>状态</th><th>负责人</th><th>更新时间</th><th>操作</th></tr></thead><tbody><tr v-for="i in 8"><td>NO-202405-00{{i}}</td><td>某能源集团有限公司</td><td>数字化平台项目</td><td><span class="pill blue">处理中</span></td><td>张伟</td><td>2024-05-2{{i}} 14:30</td><td><a>查看</a> <a>编辑</a></td></tr></tbody></table></section><section class="card"><h3>模块说明</h3><p>该页面用于管理 {{title}} 相关数据，支持按租户隔离、按角色授权、按组织查看下属数据，并保留完整操作日志。</p><div class="quick"><button class="primary">新增</button><button>导出</button><button>批量操作</button></div></section></div></div>`,components:{PageHeader,FilterBar}})
</script>
