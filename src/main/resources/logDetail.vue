<template>
  <ui-container class="page-system_logDetail" v-loading="rule.loading">
    <div class="riskModel-tree">
      <ui-tree
        :data="treeData"
        :props="rule.props"
        @ready="onReadyTree"
        @node-click="onTreeNodeClickHandler"/>
    </div>
    <div class="result-content" v-if="returnData">
      <p class="result-text">执行结果：<span style="color: #ff0000" v-text="returnData.result"/></p>
      <p class="result-text">执行时间：<span style="color: #ff0000" v-text="returnData.total"/></p>
      <div class="result-pre flex">
        <div class="flex__item ofh" style="flex:3;">
          <pre v-html="returnFunction"></pre>
        </div>
        <div class="flex__item ofh" style="margin-left: 3rem;">
          <pre v-html="dataModel"></pre>
        </div>
      </div>
    </div>
  </ui-container>
</template>
<script type="text/javascript" meta="{breadcrumb:[{label:'系统设置'},{label:'日志查询'}],active:'/system/log'}">
  import {js as jsBeautify} from 'js-beautify'
  import { Base64 } from 'js-base64';
  import { debug } from 'util'
  // console.log(hextoString,'840547939792052229')
  const jsBeautifyHandler = str => jsBeautify(str, {indent_size: 2})

  export default {
    name: 'PageSystemLogDetail',
    props: {
      queryId: [String, Number]
    },
    data() {
      return {
        // queryId: this.$route.params.id,
        rule: {
          loading: true,
          data: [],
          //树的字段名字配置
          props: {
            id: 'method',
            label: 'result',
            parentId: 'parent',
            method: 'method',
            format: this.propsFormat
          }
        },
        returnData: null
      }
    },
    methods: {
      propsFormat({method, name}) {
        return `${method}<br>规则名称: ${name || ""}`
      },
      onReadyTree(instance) {
        const root = instance.get_root() || {}
        const children = root.children || []
        children.length && children.forEach(item => {
          instance.collapse_node(item)
        })
      },
      // 获取当前页面的数据
      getPageData() {
        const {$fetch, queryId} = this
        const setData = (f, v) => this.$set(this.rule, f, v)

        $fetch({
          url: `/risk/logsDetail/${queryId}`,
          before: () => {
            setData('loading', true)
          },
          success: ({payload = {}}) => {
            setData('data', payload)
          },
          complete: () => {
            setData('loading', false)
          }
        })
      },
      //节点点击
      onTreeNodeClickHandler(data) {
        this.returnData = data
      }
    },
    computed: {
      treeData() {
        const tracer = (this.rule.data || {}).tracerItems
        let data
        try {
          data = tracer
          //去重处理
          let obj = {}
          data = data.reduce((item, next) => {
            obj[next.method] ? '' : obj[next.method] = true && item.push(next)
            return item
          }, [])
        } catch (e) {
          data = []
        }
        return data
      },
      dataModel() {
        const dataModel = (this.rule.data || {}).propertyModel
        const script = (this.returnData || {}).script
        if (!dataModel || !script) return ''
        let decode_scrip = Base64.decode(script)
        var in_use_prop = {}
        Base64.decode(script).replace(/data\[\"(\w+)\"\]/g,function(s1,s2){
            in_use_prop[s2] = dataModel[s2] || '缺失值'
            return s1
        })
        let data = JSON.stringify(in_use_prop)
        return jsBeautifyHandler(data)
      },
      returnFunction() {
        const _fun = (this.returnData || {}).script
        if (!_fun) return ''
        return jsBeautifyHandler(Base64.decode(_fun))
      }
    },
    watch: {
      queryId() {
        this.getPageData()
      }
    },
    created() {
      this.getPageData()
    }
  }
</script>
<style lang="scss" type="text/scss" rel="stylesheet/scss">
  .page-system_logDetail {
    .riskModel-tree {
      height: 750px;
    }

    .result-content {
      padding: 20px;
      font-size: 15px;

      .result-text {
        margin-bottom: 20px;
      }
    }
  }
</style>
