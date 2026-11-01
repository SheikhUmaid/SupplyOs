import { act, fireEvent, render, screen } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";
import { useUiStore } from "@/stores/ui/uiStore";
import { Button } from "./Button";
import { DataTable, type Column } from "./DataTable";
import { Input } from "./FormControls";
import { Drawer, Modal } from "./Overlays";
import { Pagination } from "./Pagination";
import { Toaster } from "./Toaster";

interface Row {
  id: number;
  name: string;
}

const columns: Column<Row>[] = [
  { key: "name", header: "Name", cell: (row) => row.name, sortable: true },
];

describe("DataTable", () => {
  const base = { columns, rowKey: (row: Row) => row.id, caption: "Products" };

  it("renders rows", () => {
    render(<DataTable {...base} rows={[{ id: 1, name: "Laptop" }]} />);
    expect(screen.getByRole("cell", { name: "Laptop" })).toBeInTheDocument();
  });

  it("shows the empty state only once loading has finished", () => {
    const { rerender } = render(<DataTable {...base} rows={[]} loading emptyTitle="No products" />);
    expect(screen.queryByText("No products")).not.toBeInTheDocument();
    rerender(<DataTable {...base} rows={[]} emptyTitle="No products" />);
    expect(screen.getByText("No products")).toBeInTheDocument();
  });

  it("shows the error state with a working retry", () => {
    const onRetry = vi.fn();
    render(<DataTable {...base} rows={[]} error="Server down" onRetry={onRetry} />);
    expect(screen.getByRole("alert")).toHaveTextContent("Server down");
    fireEvent.click(screen.getByRole("button", { name: "Try again" }));
    expect(onRetry).toHaveBeenCalledOnce();
  });

  it("toggles the sort direction", () => {
    const onSortChange = vi.fn();
    render(
      <DataTable
        {...base}
        rows={[{ id: 1, name: "Laptop" }]}
        sort={{ key: "name", direction: "asc" }}
        onSortChange={onSortChange}
      />,
    );
    expect(screen.getByRole("columnheader")).toHaveAttribute("aria-sort", "ascending");
    fireEvent.click(screen.getByRole("button", { name: "Name" }));
    expect(onSortChange).toHaveBeenCalledWith({ key: "name", direction: "desc" });
  });

  it("opens a row by click and by keyboard", () => {
    const onRowClick = vi.fn();
    const row = { id: 1, name: "Laptop" };
    render(<DataTable {...base} rows={[row]} onRowClick={onRowClick} />);
    const tableRow = screen.getByRole("cell", { name: "Laptop" }).closest("tr")!;
    fireEvent.click(tableRow);
    fireEvent.keyDown(tableRow, { key: "Enter" });
    expect(onRowClick).toHaveBeenCalledTimes(2);
  });
});

describe("Pagination", () => {
  const onPageChange = vi.fn();
  const base = { size: 20, totalElements: 45, totalPages: 3, onPageChange };

  it("shows the visible range, capped at the total", () => {
    render(<Pagination {...base} page={2} />);
    expect(screen.getByText("41–45 of 45")).toBeInTheDocument();
  });

  it("disables moving past either end", () => {
    const { rerender } = render(<Pagination {...base} page={0} />);
    expect(screen.getByRole("button", { name: "Previous page" })).toBeDisabled();
    rerender(<Pagination {...base} page={2} />);
    expect(screen.getByRole("button", { name: "Next page" })).toBeDisabled();
  });

  it("renders nothing for an empty result", () => {
    const { container } = render(
      <Pagination {...base} totalElements={0} totalPages={0} page={0} />,
    );
    expect(container).toBeEmptyDOMElement();
  });
});

describe("Modal", () => {
  it("is labelled, closes on Escape and returns focus to the opener", () => {
    const onClose = vi.fn();
    function Harness({ open }: { open: boolean }) {
      return (
        <>
          <button>Opener</button>
          <Modal open={open} title="Edit product" onClose={onClose}>
            <input aria-label="Name" />
          </Modal>
        </>
      );
    }
    const { rerender } = render(<Harness open={false} />);
    const opener = screen.getByRole("button", { name: "Opener" });
    opener.focus();

    rerender(<Harness open />);
    expect(screen.getByRole("dialog", { name: "Edit product" })).toBeInTheDocument();
    expect(opener).not.toHaveFocus();
    expect(document.body.style.overflow).toBe("hidden");

    fireEvent.keyDown(document, { key: "Escape" });
    expect(onClose).toHaveBeenCalledOnce();
  });
});

describe("overlay placement", () => {
  it("renders into body so a parent's spacing styles can't shrink it", () => {
    render(
      <div className="space-y-4" data-testid="parent">
        <Drawer open title="Warehouse details" onClose={() => {}}>
          <p>Body</p>
        </Drawer>
      </div>,
    );
    const dialog = screen.getByRole("dialog", { name: "Warehouse details" });
    expect(dialog.parentElement).toBe(document.body);
    expect(screen.getByTestId("parent")).toBeEmptyDOMElement();
  });
});

describe("Input", () => {
  it("links its error message and marks itself invalid", () => {
    render(<Input label="SKU" error="Already in use" />);
    const input = screen.getByLabelText("SKU");
    expect(input).toHaveAttribute("aria-invalid", "true");
    expect(input).toHaveAccessibleDescription("Already in use");
  });
});

describe("Button", () => {
  it("cannot be clicked while loading", () => {
    render(<Button loading>Save</Button>);
    expect(screen.getByRole("button", { name: "Save" })).toBeDisabled();
  });
});

describe("Toaster", () => {
  afterEach(() => {
    vi.useRealTimers();
    useUiStore.setState({ toasts: [] });
  });

  it("shows a pushed toast and removes it automatically", () => {
    vi.useFakeTimers();
    render(<Toaster />);
    act(() => useUiStore.getState().pushToast("success", "Order confirmed"));
    expect(screen.getByText("Order confirmed")).toBeInTheDocument();

    act(() => vi.advanceTimersByTime(5000));
    expect(useUiStore.getState().toasts).toEqual([]);
  });
});
