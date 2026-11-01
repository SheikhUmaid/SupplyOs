"use client";

import { Package } from "lucide-react";
import Image from "next/image";
import { useState } from "react";

/** Falls back to an icon when a product has no image or its address no longer works. */
export function ProductImage({
  src,
  alt,
  sizes,
}: {
  src: string | null;
  alt: string;
  sizes: string;
}) {
  const [failed, setFailed] = useState(false);

  return (
    <div className="relative aspect-square w-full overflow-hidden rounded-md bg-canvas">
      {src && !failed ? (
        // Administrators can enter any address, so Next's domain-limited optimizer isn't used.
        <Image
          src={src}
          alt={alt}
          fill
          sizes={sizes}
          unoptimized
          className="object-cover"
          onError={() => setFailed(true)}
        />
      ) : (
        <div className="flex h-full items-center justify-center">
          <Package
            role="img"
            aria-label={alt}
            className="size-1/3 text-ink-subtle"
            strokeWidth={1}
          />
        </div>
      )}
    </div>
  );
}
