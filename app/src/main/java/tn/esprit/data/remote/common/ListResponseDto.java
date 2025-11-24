package tn.esprit.data.remote.common;

import java.util.List;

/**
 * Generic list wrapper matching backend dto.common.ListResponse<T>.
 *
 * JSON example:
 * {
 *   "items": [ ... ],
 *   "total": 3
 * }
 */
public class ListResponseDto<T> {

    private List<T> items;
    private long total;

    public ListResponseDto() {
    }

    public List<T> getItems() {
        return items;
    }

    public void setItems(List<T> items) {
        this.items = items;
    }

    public long getTotal() {
        return total;
    }

    public void setTotal(long total) {
        this.total = total;
    }
}
