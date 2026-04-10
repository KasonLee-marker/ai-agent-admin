declare module 'react-markdown' {
    import {ComponentType} from 'react'

    interface ReactMarkdownProps {
        children?: string
        remarkPlugins?: any[]
        rehypePlugins?: any[]
        components?: Record<string, ComponentType<any>>
    }

    const ReactMarkdown: ComponentType<ReactMarkdownProps>
    export default ReactMarkdown
}

declare module 'remark-gfm' {
    const remarkGfm: any
    export default remarkGfm
}

declare module 'rehype-highlight' {
    const rehypeHighlight: any
    export default rehypeHighlight
}