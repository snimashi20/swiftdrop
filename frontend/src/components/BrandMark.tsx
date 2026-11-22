/**
 * The SwiftDrop parcel mark — the same artwork as /public/favicon.svg.
 *
 * Decorative by default: every call site renders it beside the "swiftdrop"
 * wordmark, so the brand is already announced as text and the icon is hidden
 * from screen readers to avoid a duplicate reading. Pass a `title` to make it
 * a labelled image when it is ever used on its own.
 */
export function BrandMark({
  size = 26,
  className = "inline-block align-middle mr-2",
  title,
}: {
  size?: number;
  className?: string;
  title?: string;
}) {
  return (
    <svg
      xmlns="http://www.w3.org/2000/svg"
      viewBox="0 0 64 64"
      width={size}
      height={size}
      className={className}
      role={title ? "img" : undefined}
      aria-label={title}
      aria-hidden={title ? undefined : true}
      focusable="false"
    >
      {title ? <title>{title}</title> : null}
      {/* Navy site palette tile */}
      <rect width="64" height="64" rx="14" fill="#12252B" />
      {/* Bold mint parcel: readable down to 16 pixels */}
      <path d="M17 25.5 32 17 47 25.5 V43 L32 51 17 43 Z" fill="#C9E8D5" />
      {/* Subtle parcel fold creates recognisable three-dimensional box */}
      <path
        d="M17 25.5 32 34 47 25.5"
        fill="none"
        stroke="#12252B"
        strokeWidth="2.6"
        strokeLinejoin="round"
      />
      <path d="M32 34 V51" fill="none" stroke="#12252B" strokeWidth="2.6" />
      {/* Terracotta packing tape is the distinctive SwiftDrop brand accent */}
      <path
        d="M27.5 19.6 32 17 36.5 19.6 36.5 31.4 32 29 27.5 31.4 Z"
        fill="#C25D47"
      />
    </svg>
  );
}
