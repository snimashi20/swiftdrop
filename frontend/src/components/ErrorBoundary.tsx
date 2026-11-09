import { Component, type ErrorInfo, type ReactNode } from "react";

type ErrorBoundaryProps = {
  children: ReactNode;
  fallback?: ReactNode | ((error: Error, reset: () => void) => ReactNode);
};

type ErrorBoundaryState = {
  error: Error | null;
};

export class ErrorBoundary extends Component<ErrorBoundaryProps, ErrorBoundaryState> {
  state: ErrorBoundaryState = { error: null };

  static getDerivedStateFromError(error: Error): ErrorBoundaryState {
    return { error };
  }

  componentDidCatch(error: Error, errorInfo: ErrorInfo) {
    console.error("Unhandled UI error", { error, componentStack: errorInfo.componentStack });
  }

  reset = () => this.setState({ error: null });

  render() {
    if (!this.state.error) return this.props.children;
    if (typeof this.props.fallback === "function") return this.props.fallback(this.state.error, this.reset);
    if (this.props.fallback) return this.props.fallback;
    return (
      <div role="alert" className="rounded-2xl border border-red-200 bg-red-50 p-6 text-red-950">
        <h2 className="font-display text-xl font-bold">Something went wrong</h2>
        <p className="mt-2 text-sm opacity-80">This view could not be loaded.</p>
        <button type="button" onClick={this.reset} className="mt-4 rounded-lg bg-ink px-4 py-2 text-sm font-semibold text-white hover:bg-ink/90">Try again</button>
      </div>
    );
  }
}