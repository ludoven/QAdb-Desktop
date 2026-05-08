import { defineConfig } from 'vitepress'

export default defineConfig({
  title: 'QAdb-Desktop',
  description: '开源、跨平台、现代化的 ADB 桌面调试工具',
  lang: 'zh-CN',
  cleanUrls: true,
  lastUpdated: true,
  head: [
    ['meta', { name: 'theme-color', content: '#2563eb' }],
    ['meta', { property: 'og:title', content: 'QAdb-Desktop' }],
    ['meta', { property: 'og:description', content: '让常用 ADB 操作变得更直观、更高效。' }],
    ['meta', { property: 'og:type', content: 'website' }]
  ],
  themeConfig: {
    logo: '/logo.svg',
    siteTitle: 'QAdb',
    nav: [
      { text: '首页', link: '/' },
      { text: '功能', link: '/features' },
      { text: '快速开始', link: '/guide/getting-started' },
      { text: '下载', link: '/download' },
      { text: 'GitHub', link: 'https://github.com/ludoven/QAdb-Desktop' }
    ],
    sidebar: {
      '/guide/': [
        {
          text: '指南',
          items: [
            { text: '快速开始', link: '/guide/getting-started' }
          ]
        }
      ],
      '/': [
        {
          text: 'QAdb',
          items: [
            { text: '功能亮点', link: '/features' },
            { text: '下载', link: '/download' }
          ]
        }
      ]
    },
    socialLinks: [
      { icon: 'github', link: 'https://github.com/ludoven/QAdb-Desktop' }
    ],
    search: {
      provider: 'local'
    },
    footer: {
      message: '基于 Jetpack Compose Multiplatform 构建',
      copyright: 'Released under the open-source license.'
    }
  }
})
