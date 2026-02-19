import { withMermaid } from "vitepress-plugin-mermaid";

export default withMermaid({
    title: 'Dataportal Backend',
    description: 'Dataportal Backend Documentation',
    ignoreDeadLinks: true,
    base: process.env.DOCS_BASE || '/',
    appearance: true,
    lastUpdated: true,
    themeConfig: {
        siteTitle: false,

        editLink: {
            pattern: 'https://github.com/medizininformatik-initiative/dataportal-backend/edit/main/docs/:path',
            text: 'Edit this page on GitHub'
        },

        socialLinks: [
            { icon: 'github', link: 'https://github.com/medizininformatik-initiative/dataportal-backend' }
        ],

        footer: {
            message: 'Released under the <a href="https://www.apache.org/licenses/LICENSE-2.0">Apache License 2.0</a>',
        },

        search: {
            provider: 'local'
        },

        outline: {
            level: [2, 3]
        },

        nav: [
            { text: 'Home', link: '/' }
        ],

        sidebar: [
            {
                text: 'Home',
                link: '/index.md',
                activeMatch: '^/$'
            },
            {
                text: 'Overview',
                link: '/overview.md',
                activeMatch: '^/$'
            },
            {
                text: 'Configuration',
                link: '/configuration.md',
                activeMatch: '^/$'
            },
            {
                text: 'API',
                link: '/api.md',
                activeMatch: '^/$'
            },
            {
                text: 'Use',
                link: '/use.md',
                activeMatch: '^/$'
            },
            {
                text: 'Development Setup',
                link: '/dev.md',
                activeMatch: '^/$'
            }
        ]
    }
})