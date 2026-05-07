import { useState } from "react"
import { useNavigate, useLocation } from "react-router-dom"
import InputBasic from "@/components/InputBasic.tsx"
import AddressSearchTip from "@/features/address/components/AddressSearchTip"
import AddressSearchResults from "@/features/address/components/AddressSearchResults"
import type { JusoItem } from "@/features/address/types"
import { validateKeyword } from "@/features/address/utils/validateKeyword.ts"

export default function AddressSearchPage() {
    const [keyword, setKeyword] = useState("")
    const navigate = useNavigate()
    const location = useLocation()

    const handleSelectAddress = (item: JusoItem) => {
        const from = (location.state as { from?: string } | null)?.from
        if (!from) return
        navigate(from, {
            state: { address: item.roadAddrPart1, buildingName: item.bdNm },
            replace: true,
        })
    }

    return (
        <>
            <InputBasic
                placeholder="예) 판교역로 166, 분당 주공, 백현동 532"
                value={keyword}
                onChange={(val) => {
                    setKeyword(validateKeyword(val))
                }}
                isClearable={true}
            />

            {!keyword ? (
                <AddressSearchTip />
            ) : (
                <AddressSearchResults keyword={keyword} onSelect={handleSelectAddress} />
            )}
        </>
    )
}
