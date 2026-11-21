import { describe, expect, it } from "vitest";
import { addressFromReverse, findDistrict, reverseFromPhoton } from "./placeAddress";

describe("findDistrict", () => {
  it("matches a district name with or without the word District", () => {
    expect(findDistrict("Colombo District")).toBe("Colombo");
    expect(findDistrict("gampaha")).toBe("Gampaha");
  });

  it("matches a different spelling", () => {
    expect(findDistrict("Kegalla District")).toBe("Kegalle");
  });

  it("returns null for an unknown place", () => {
    expect(findDistrict("Atlantis District")).toBeNull();
    expect(findDistrict(undefined)).toBeNull();
  });
});

describe("addressFromReverse", () => {
  it("builds an address from a Nominatim answer", () => {
    const address = addressFromReverse({
      display_name: "12, Galle Road, Bambalapitiya, Colombo, Sri Lanka",
      address: {
        house_number: "12",
        road: "Galle Road",
        suburb: "Bambalapitiya",
        city: "Colombo",
        state_district: "Colombo District",
        country_code: "lk",
      },
    });
    expect(address).toEqual({ line1: "12, Galle Road, Bambalapitiya", city: "Colombo", district: "Colombo" });
  });

  it("uses the start of the full address when there is no road", () => {
    const address = addressFromReverse({
      display_name: "Viharamahadevi Park, Colombo, Western Province, Sri Lanka",
      address: { city: "Colombo", state_district: "Colombo District", country_code: "lk" },
    });
    expect(address?.line1).toBe("Viharamahadevi Park, Colombo");
  });

  it("finds the district from the county when state_district is missing", () => {
    const address = addressFromReverse({
      address: { road: "Main Street", town: "Gampaha", county: "Gampaha", country_code: "lk" },
    });
    expect(address).toEqual({ line1: "Main Street", city: "Gampaha", district: "Gampaha" });
  });

  it("returns null outside Sri Lanka", () => {
    expect(addressFromReverse({ address: { road: "Main St", city: "Colombo", country_code: "us" } })).toBeNull();
  });

  it("returns null outside the delivery districts", () => {
    expect(
      addressFromReverse({ address: { road: "A Road", state_district: "Atlantis District", country_code: "lk" } }),
    ).toBeNull();
  });
});

describe("reverseFromPhoton", () => {
  it("turns a Photon place into an address", () => {
    const address = addressFromReverse(
      reverseFromPhoton({
        housenumber: "12",
        street: "Galle Road",
        district: "Bambalapitiya",
        city: "Colombo",
        county: "Colombo District",
        countrycode: "LK",
      }),
    );
    expect(address).toEqual({ line1: "12, Galle Road, Bambalapitiya", city: "Colombo", district: "Colombo" });
  });

  it("uses the place name when there is no street", () => {
    const address = addressFromReverse(
      reverseFromPhoton({ name: "Viharamahadevi Park", city: "Colombo", countrycode: "LK" }),
    );
    expect(address?.line1).toBe("Viharamahadevi Park, Colombo");
  });
});
