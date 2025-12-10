import Link from "@docusaurus/Link"
import useDocusaurusContext from "@docusaurus/useDocusaurusContext"
import Layout from "@theme/Layout"

function HomepageHeader() {
  const { siteConfig } = useDocusaurusContext()
  return (
    <header className="hero hero--primary" style={{ padding: "4rem 0" }}>
      <div className="container">
        <h1 className="hero__title">{siteConfig.title}</h1>
        <p className="hero__subtitle">{siteConfig.tagline}</p>
        <div style={{ display: "flex", gap: "1rem", justifyContent: "center" }}>
          <Link className="button button--secondary button--lg" to="/docs/getting-started">
            Get Started
          </Link>
          <Link
            className="button button--outline button--lg"
            style={{ color: "white", borderColor: "white" }}
            to="/docs/architecture"
          >
            Architecture
          </Link>
        </div>
      </div>
    </header>
  )
}

function Feature({
  title,
  description,
  link,
}: {
  title: string
  description: string
  link: string
}) {
  return (
    <div className="col col--4">
      <div className="card" style={{ height: "100%", padding: "1.5rem" }}>
        <h3>{title}</h3>
        <p>{description}</p>
        <Link to={link}>Learn more →</Link>
      </div>
    </div>
  )
}

export default function Home() {
  return (
    <Layout description="FactoryFone Admin Portal Documentation">
      <HomepageHeader />
      <main style={{ padding: "2rem 0" }}>
        <div className="container">
          <div className="row">
            <Feature
              title="Frontend (Next.js)"
              description="React 19 with App Router, Tailwind CSS, and shadcn/ui components."
              link="/docs/frontend/overview"
            />
            <Feature
              title="Backend (Spring Boot)"
              description="Java 25 REST API with JPA, SQLite, and OpenAPI documentation."
              link="/docs/backend/overview"
            />
            <Feature
              title="Development"
              description="Local setup, testing, dev containers, and CI/CD pipeline."
              link="/docs/development/setup"
            />
          </div>
        </div>
      </main>
    </Layout>
  )
}
