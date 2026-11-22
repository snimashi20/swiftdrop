import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { MemoryRouter } from "react-router-dom";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { registerMerchant } from "../api";
import { MerchantRegisterPage } from "./MerchantRegisterPage";

vi.mock("../api", () => ({
  registerMerchant: vi.fn(),
}));

const validForm = {
  "Registered business name *": "Northstar Goods",
  "Business registration number *": "BR-123456",
  "Contact person *": "Alex Morgan",
  "Business email / sign-in email *": "orders@example.test",
  "Password *": "test-password-123",
  "Sri Lankan mobile number *": "0771234567",
  "Registered or operating address *": "12 Main Street, Colombo 03",
};

function renderPage() {
  return render(<MemoryRouter><MerchantRegisterPage /></MemoryRouter>);
}

async function fillValidForm() {
  const user = userEvent.setup();
  for (const [label, value] of Object.entries(validForm)) {
    await user.type(screen.getByLabelText(label), value);
  }
  await user.selectOptions(screen.getByLabelText("Business structure *"), "PRIVATE_LIMITED_COMPANY");
  await user.type(screen.getByLabelText("Products or services *"), "Retail sale of household goods and gifts.");
  await user.click(screen.getByRole("checkbox", { name: /i confirm that i am authorized/i }));
  return user;
}

describe("MerchantRegisterPage", () => {
  beforeEach(() => {
    vi.mocked(registerMerchant).mockResolvedValue({
      id: 41,
      businessName: "Northstar Goods",
      registrationNumber: "BR-123456",
      contactName: "Alex Morgan",
      contactEmail: "orders@example.test",
      contactPhone: "0771234567",
      status: "PENDING",
      createdAt: "2026-11-08T10:00:00Z",
    });
  });

  it("shows required-field validation and does not call the API", async () => {
    renderPage();
    await userEvent.setup().click(screen.getByRole("button", { name: /submit business application/i }));

    expect(await screen.findByText("Enter your registered business name")).toBeInTheDocument();
    expect(screen.getByText("Select a business type")).toBeInTheDocument();
    expect(screen.getByText("Enter your business registration number")).toBeInTheDocument();
    expect(screen.getByText("Enter a contact name")).toBeInTheDocument();
    expect(screen.getByText("Enter a valid email address")).toBeInTheDocument();
    expect(screen.getByText("Use at least 8 characters")).toBeInTheDocument();
    expect(screen.getByText("Use 07XXXXXXXX or +947XXXXXXXX")).toBeInTheDocument();
    expect(screen.getByText("Enter the registered or operating business address")).toBeInTheDocument();
    expect(screen.getByText("Describe your products or services (at least 10 characters)")).toBeInTheDocument();
    expect(screen.getByText("Confirm that the business information is accurate")).toBeInTheDocument();
    expect(registerMerchant).not.toHaveBeenCalled();
  });

  it("rejects malformed email and phone input", async () => {
    renderPage();
    const user = await fillValidForm();
    fireEvent.change(screen.getByLabelText("Business email / sign-in email *"), { target: { value: "not-an-email" } });
    fireEvent.change(screen.getByLabelText("Sri Lankan mobile number *"), { target: { value: "12345" } });
    await user.click(screen.getByRole("button", { name: /submit business application/i }));

    expect(await screen.findByText("Enter a valid email address")).toBeInTheDocument();
    expect(screen.getByText("Use 07XXXXXXXX or +947XXXXXXXX")).toBeInTheDocument();
    expect(registerMerchant).not.toHaveBeenCalled();
  });

  it("submits valid data and confirms the application is pending", async () => {
    renderPage();
    const user = await fillValidForm();
    await user.click(screen.getByRole("button", { name: /submit business application/i }));

    expect(await screen.findByText("Application received")).toBeInTheDocument();
    expect(screen.getByText(/remain pending until it is approved/i)).toBeInTheDocument();
    expect(registerMerchant).toHaveBeenCalledWith({
      businessName: "Northstar Goods",
      businessType: "PRIVATE_LIMITED_COMPANY",
      registrationNumber: "BR-123456",
      businessAddress: "12 Main Street, Colombo 03",
      businessDescription: "Retail sale of household goods and gifts.",
      verificationDeclarationAccepted: true,
      contactName: "Alex Morgan",
      email: "orders@example.test",
      password: "test-password-123",
      contactPhone: "0771234567",
    });
  }, 10000);

  it("shows duplicate registration errors from the API", async () => {
    vi.mocked(registerMerchant).mockRejectedValue(new Error("A merchant with this email already exists"));
    renderPage();
    const user = await fillValidForm();
    await user.click(screen.getByRole("button", { name: /submit business application/i }));

    expect(await screen.findByRole("alert")).toHaveTextContent("A merchant with this email already exists");
  });

  it("disables submission while registration is pending", async () => {
    let resolveRegistration!: (value: Awaited<ReturnType<typeof registerMerchant>>) => void;
    vi.mocked(registerMerchant).mockReturnValue(new Promise((resolve) => { resolveRegistration = resolve; }));
    renderPage();
    const user = await fillValidForm();
    await user.click(screen.getByRole("button", { name: /submit business application/i }));

    const submit = await screen.findByRole("button", { name: /submitting application/i });
    expect(submit).toBeDisabled();
    resolveRegistration({
      id: 41,
      businessName: "Northstar Goods",
      registrationNumber: "BR-123456",
      contactName: "Alex Morgan",
      contactEmail: "orders@example.test",
      contactPhone: "0771234567",
      status: "PENDING",
      createdAt: "2026-11-08T10:00:00Z",
    });
    await waitFor(() => expect(screen.getByText("Application received")).toBeInTheDocument());
  });
});
