import { StatusBadge } from "@/components/ui/Badge";
import type { Tracking } from "@/types/api";
import { formatDateTime } from "@/utils/format";
import { SHIPMENT_STATUS } from "@/utils/status";

/** Every recorded change, with who made it. This is the operational detail administrators get. */
export function ShipmentHistory({ history }: { history: Tracking["history"] }) {
  return (
    <div className="overflow-x-auto">
      <table className="w-full border-collapse text-left text-[13px]">
        <caption className="px-1 pb-1.5 text-left text-xs font-semibold text-ink">
          Status history
        </caption>
        <thead>
          <tr className="border-y border-line bg-surface-muted text-ink-muted">
            <th scope="col" className="px-3 py-1.5 font-medium">
              When
            </th>
            <th scope="col" className="px-3 py-1.5 font-medium">
              Status
            </th>
            <th scope="col" className="px-3 py-1.5 font-medium">
              By
            </th>
          </tr>
        </thead>
        <tbody>
          {history.map((entry) => (
            <tr
              key={`${entry.toStatus}-${entry.at}`}
              className="border-b border-line last:border-0"
            >
              <td className="px-3 py-2 whitespace-nowrap">{formatDateTime(entry.at)}</td>
              <td className="px-3 py-2">
                <StatusBadge status={SHIPMENT_STATUS[entry.toStatus]} />
                {entry.note && <p className="mt-0.5 text-xs text-ink-muted">{entry.note}</p>}
              </td>
              <td className="px-3 py-2">{entry.updatedBy}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}
