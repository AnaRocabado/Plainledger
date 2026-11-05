import { render, screen } from "@testing-library/react";
import App from "./App.tsx";

describe("App shell", () => {
  it("[FOUND-AC-08] shows the product name and the guiding principle", () => {
    render(<App />);

    expect(screen.getByRole("heading", { level: 1, name: "Plainledger" })).toBeInTheDocument();
    expect(screen.getByText("Show the real numbers, not the flattering ones.")).toBeInTheDocument();
  });
});
