import { ChevronDown, ChevronLeft, ChevronRight, ChevronsUpDown } from "lucide-react";
import { useState, type ReactNode } from "react";

export type DataTableColumn<T> = {
  key: string;
  header: string;
  cell: (row: T) => ReactNode;
  sortValue?: (row: T) => string | number | Date | null | undefined;
  sortable?: boolean;
  className?: string;
};

export type DataTableProps<T> = {
  columns: DataTableColumn<T>[];
  data: T[];
  rowKey: (row: T, index: number) => string | number;
  pageSize?: number;
  emptyState?: ReactNode;
  caption?: string;
};

type SortState = {
  key: string;
  direction: "asc" | "desc";
};

export function DataTable<T>({
  columns,
  data,
  rowKey,
  pageSize = 10,
  emptyState = "No records found.",
  caption,
}: DataTableProps<T>) {
  const [sort, setSort] = useState<SortState | null>(null);
  const [page, setPage] = useState(0);
  const safePageSize = Math.max(1, pageSize);
  const sortedData = sort
    ? [...data].sort((first, second) => compareValues(
        columns.find((column) => column.key === sort.key)?.sortValue?.(first),
        columns.find((column) => column.key === sort.key)?.sortValue?.(second),
      ) * (sort.direction === "asc" ? 1 : -1))
    : data;
  const pageCount = Math.max(1, Math.ceil(sortedData.length / safePageSize));
  const currentPage = Math.min(page, pageCount - 1);
  const visibleRows = sortedData.slice(currentPage * safePageSize, (currentPage + 1) * safePageSize);

  function changeSort(column: DataTableColumn<T>) {
    if (!column.sortable || !column.sortValue) return;
    setSort((current) => current?.key === column.key
      ? { key: column.key, direction: current.direction === "asc" ? "desc" : "asc" }
      : { key: column.key, direction: "asc" });
    setPage(0);
  }

  return (
    <div className="overflow-hidden rounded-2xl border border-ink/10 bg-white shadow-sm">
      <div className="overflow-x-auto">
        <table className="min-w-full divide-y divide-ink/10 text-left text-sm">
          {caption && <caption className="sr-only">{caption}</caption>}
          <thead className="bg-ink/[0.03] text-xs uppercase tracking-[0.12em] text-ink/50">
            <tr>
              {columns.map((column) => (
                <th key={column.key} scope="col" className={`whitespace-nowrap px-4 py-3 font-bold ${column.className ?? ""}`}>
                  {column.sortable && column.sortValue ? (
                    <button
                      type="button"
                      className="inline-flex items-center gap-1.5 rounded-md py-1 text-left hover:text-ink focus:outline-none focus:ring-2 focus:ring-coral/40"
                      onClick={() => changeSort(column)}
                      aria-label={`Sort by ${column.header}`}
                    >
                      {column.header}
                      {sort?.key === column.key
                        ? <ChevronDown size={14} className={sort.direction === "asc" ? "rotate-180" : ""} aria-hidden="true" />
                        : <ChevronsUpDown size={14} aria-hidden="true" />}
                    </button>
                  ) : column.header}
                </th>
              ))}
            </tr>
          </thead>
          <tbody className="divide-y divide-ink/10">
            {visibleRows.length > 0 ? visibleRows.map((row, index) => (
              <tr key={rowKey(row, currentPage * safePageSize + index)} className="transition hover:bg-mist/70">
                {columns.map((column) => <td key={column.key} className={`px-4 py-3.5 ${column.className ?? ""}`}>{column.cell(row)}</td>)}
              </tr>
            )) : (
              <tr><td colSpan={columns.length} className="px-4 py-12 text-center text-sm text-ink/50">{emptyState}</td></tr>
            )}
          </tbody>
        </table>
      </div>
      <div className="flex items-center justify-between border-t border-ink/10 px-4 py-3 text-xs text-ink/50">
        <span>{data.length === 0 ? "0 records" : `${currentPage * safePageSize + 1}-${Math.min((currentPage + 1) * safePageSize, data.length)} of ${data.length}`}</span>
        <div className="flex items-center gap-2">
          <button type="button" className="rounded-md p-1.5 hover:bg-ink/5 disabled:cursor-not-allowed disabled:opacity-30" onClick={() => setPage((value) => Math.max(0, value - 1))} disabled={currentPage === 0} aria-label="Previous page"><ChevronLeft size={16} /></button>
          <span aria-live="polite">Page {currentPage + 1} of {pageCount}</span>
          <button type="button" className="rounded-md p-1.5 hover:bg-ink/5 disabled:cursor-not-allowed disabled:opacity-30" onClick={() => setPage((value) => Math.min(pageCount - 1, value + 1))} disabled={currentPage >= pageCount - 1} aria-label="Next page"><ChevronRight size={16} /></button>
        </div>
      </div>
    </div>
  );
}

function compareValues(first: string | number | Date | null | undefined, second: string | number | Date | null | undefined) {
  if (first == null && second == null) return 0;
  if (first == null) return 1;
  if (second == null) return -1;
  const firstValue = first instanceof Date ? first.getTime() : first;
  const secondValue = second instanceof Date ? second.getTime() : second;
  return firstValue < secondValue ? -1 : firstValue > secondValue ? 1 : 0;
}