import {showToast} from "@/lib/notify.ts";

export function validateKeyword (value: string): string {
    if (!value) return value

    const specialRegex = /[%=><]/g
    if (specialRegex.test(value)) {
        showToast({ message: "% = > < 와 같은 특수문자는 사용할 수 없습니다." })
        return value.replace(specialRegex, "")
    }

    const sqlKeywords = [
        "OR", "SELECT", "INSERT", "DELETE",
        "UPDATE", "CREATE", "DROP", "EXEC",
        "UNION", "FETCH", "DECLARE", "TRUNCATE"
    ]

    let filtered = value

    for (const word of sqlKeywords) {
        if (new RegExp(word, "gi").test(filtered)) {
            showToast({ message: `${word}와(과) 같은 문자는 사용할 수 없습니다.` })
            filtered = filtered.replace(new RegExp(word, "gi"), "")
            break
        }
    }
    return filtered
}