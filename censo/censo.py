import pandas as pd
from dateutil import parser
import datetime

CSV_PATH = "censo.csv"
SQL_PATH = "censo.sql"

def clean(value):
    if pd.isna(value):
        return None
    value = str(value).strip()
    return value or None


def coordinate(value, degree_digits):
    """Convert -74,063,064 -> -74.063064."""
    value = clean(value)
    if value is None:
        return None

    # The source uses commas as decimal-group separators.
    raw = value.replace(",", "").replace(".", "")
    sign = -1 if raw.startswith("-") else 1
    digits = raw.lstrip("-")

    if not digits.isdigit() or len(digits) <= degree_digits:
        raise ValueError(f"Invalid coordinate: {value}")

    return sign * float(
        digits[:degree_digits] + "." + digits[degree_digits:]
    )


def number(value):
    value = clean(value)
    if value is None:
        return None
    return float(value.replace(",", "."))


def observation_date(value):
    value = clean(value)
    if not value:
        return None

    # Some cells contain "0", which is not a valid date.
    if value == "0":
        return None

    try:
        # Assume day-first for dates such as 11/12/2025.
        # Incomplete dates such as "17 Dec" default to 2025.
        return parser.parse(
            value,
            dayfirst=True,
            default=parser.parse("2025-01-01"),
        ).date()
    except (ValueError, OverflowError):
        print(f"Warning: invalid date {value!r}; importing as NULL")
        return None


def health_status(value):
    value = clean(value)
    if value is None:
        return None

    # ADAPT these values to your actual Java enum constants.
    mapping = {
        "bueno": "WELL",
        "regular": "FAIR",
        "malo": "POOR",
    }
    result = mapping.get(value.lower())
    if result is None:
        raise ValueError(f"Unknown health status: {value!r}")
    return result


def main():
    # The first CSV row is a title, the second contains column names.
    df = pd.read_csv(CSV_PATH, skiprows=1)
    df.columns = [str(c).strip() for c in df.columns]

    # Helper to convert Python values to SQL literals
    def sql_literal(val, typ="text"):
        if val is None:
            return "NULL"
        if isinstance(val, str):
            escaped = val.replace("'", "''")
            return f"'{escaped}'"
        if isinstance(val, (int, float)):
            return str(val)
        if isinstance(val, datetime.date):
            return f"'{val.isoformat()}'"
        # Fallback to string representation
        escaped = str(val).replace("'", "''")
        return f"'{escaped}'"

    # Open SQL file for writing
    with open(SQL_PATH, "w", encoding="utf-8") as sql_file:
        sql_file.write("BEGIN;\n")
        imported = 0
        for row_num, row in df.iterrows():
            scientific = clean(row["Nombre cientifico"])
            common = clean(row["Nombre común"])
            family = clean(row["Familia"])

            # Ignore completely empty rows.
            if not any([scientific, common, family]):
                continue

            if not scientific or not common or not family:
                raise ValueError(
                    f"Row {row_num + 3}: missing species fields"
                )

            density = number(row["Densidad (g/cm3)"])
            if density is not None and density < 0:
                raise ValueError(f"Negative density at row {row_num + 3}")

            # Build species insert statement
            species_sql = f"""
            INSERT INTO species
                (id, scientific_name, common_name, family, wood_density_g_cm3)
            VALUES
                (gen_random_uuid(), {sql_literal(scientific)}, {sql_literal(common)}, {sql_literal(family)}, {sql_literal(density)})
            ON CONFLICT (scientific_name) DO UPDATE SET
                common_name = EXCLUDED.common_name,
                family = EXCLUDED.family,
                wood_density_g_cm3 = EXCLUDED.wood_density_g_cm3;
            """
            sql_file.write(species_sql)

            source_id = clean(row["ID"])
            if source_id is None:
                raise ValueError(
                    f"Row {row_num + 3}: missing source ID"
                )

            longitude = coordinate(row["Coordenada X"], 2)
            latitude = coordinate(row["Coordenada Y"], 1)

            if longitude is None or latitude is None:
                # Skip rows lacking coordinates – they cannot be inserted
                print(
                    f"Skipping row {row_num + 3}: missing coordinate values"
                )
                continue

            if not (-180 <= longitude <= 180):
                raise ValueError(f"Invalid longitude at row {row_num + 3}")
            if not (-90 <= latitude <= 90):
                raise ValueError(f"Invalid latitude at row {row_num + 3}")

            circumference = number(row["CAP1 (m)"])
            height = number(row["Altura h (m)"])
            health = health_status(row["Estado fitosanitario"])
            notes = clean(row["Observación"])
            observed_at = observation_date(row["Fecha"])

            specimen_sql = f"""
            INSERT INTO specimens (
                id, species_id, latitude, longitude,
                circumference_m, height_m, health_status, notes, observation_date, created_at
            )
            VALUES (
                gen_random_uuid(),
                (SELECT id FROM species WHERE scientific_name = {sql_literal(scientific)}),
                {sql_literal(latitude)}, {sql_literal(longitude)},
                {sql_literal(circumference)}, {sql_literal(height)},
                {sql_literal(health)}, {sql_literal(notes)}, {sql_literal(observed_at)},
                NOW()
            );
            """
            sql_file.write(specimen_sql)
            imported += 1

        sql_file.write("COMMIT;\n")
        print(f"Processed {imported} rows successfully. SQL written to {SQL_PATH}.")

    # The connection context commits on success and rolls back on error.

if __name__ == "__main__":
    main()
